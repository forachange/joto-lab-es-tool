package com.joto.lab.es.tool;

import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.joto.lab.es.core.config.MyBatisGeneratorConfig;
import com.joto.lab.es.core.utils.MybatisGeneratorUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.generator.api.ProgressCallback;
import org.mybatis.generator.exception.InvalidConfigurationException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author joey
 */
@Slf4j
public class Controller {

    private static final String JOTO_CONFIG = "joto.config";
    public TextField tfUrl;
    public TextField tfUser;
    public PasswordField pfPassword;
    public TextArea taTables;
    public TextArea taDomains;
    public TextField tfProjectPath;
    public TextField tfEntityPackage;
    public TextField tfServicePackage;
    public TextField tfAuthor;

    public static void openDir(String outDir) throws IOException {
        String osName = System.getProperty("os.name");
        if (osName != null) {
            if (osName.contains("Mac")) {
                Runtime.getRuntime().exec("open " + outDir);
            } else if (osName.contains("Windows")) {
                Runtime.getRuntime().exec(MessageFormat.format("cmd /c start \"\" \"{0}\"", outDir));
            } else {
                log.debug("文件输出目录:{}", outDir);
            }
        } else {
            log.warn("读取操作系统失败");
        }

    }

    @FXML
    public void generate() {

        checkTextField(tfUrl, "URL 不能为空");
        checkTextField(tfUser, "Username 不能为空");
        checkTextField(tfProjectPath, "Project Path 不能为空");
        checkTextField(tfAuthor, "Author 不能为空");
        checkTextField(tfEntityPackage, "Entity Package 不能为空");
        checkTextField(tfServicePackage, "Service Package 不能为空");

        if (StrUtil.isBlank(pfPassword.getText())) {
            showWindowAlert("Password 不能为空", Alert.AlertType.WARNING);
            throw new IllegalArgumentException("Password 不能为空");
        }

        if (StrUtil.isBlank(taTables.getText())) {
            showWindowAlert("Tables 不能为空", Alert.AlertType.WARNING);
            throw new IllegalArgumentException("Password 不能为空");
        }

        final String[] tables = taTables.getText().split(";");
        final String[] domains = taDomains.getText().split(";");

        if (tables.length != domains.length) {
            showWindowAlert("Tables 与 Domains 不匹配", Alert.AlertType.WARNING);
            throw new IllegalArgumentException("Tables 与 Domains 不匹配");
        }

        MyBatisGeneratorConfig config = new MyBatisGeneratorConfig();
        config.setMysqlUrl(tfUrl.getText());
        config.setMysqlUser(tfUser.getText());
        config.setMysqlPwd(pfPassword.getText());
        config.setTargetProject(tfProjectPath.getText());
        config.setEntityTargetPackage(tfEntityPackage.getText());
        config.setServiceTargetPackage(tfServicePackage.getText());
        config.setAuthor(tfAuthor.getText());
        config.setTables(taTables.getText());
        config.setDomains(taDomains.getText());

        try {
            MybatisGeneratorUtil.generateClassAndMapping(config, null);
        } catch (InterruptedException | SQLException | InvalidConfigurationException
                | IOException | ClassNotFoundException ex) {
            log.error(ex.getMessage(), ex);
            showWindowAlert(ex.getMessage(), Alert.AlertType.ERROR);
            return;
        }

        open(config.getTargetProject());

        final String absolutePath = new File(".").getAbsolutePath();

        File file = new File(absolutePath + File.separatorChar + JOTO_CONFIG);

        final String jsonStr = JSONUtil.toJsonStr(config);
        FileWriter fileWriter = new FileWriter(file.getAbsolutePath());
        fileWriter.write(jsonStr);

    }

    private void checkTextField(TextField tf, String message) {
        String val = tf.getText();
        if (StrUtil.isBlank(val)) {
            showWindowAlert(message, Alert.AlertType.WARNING);
            throw new IllegalArgumentException(message);
        }
    }

    public void open(String outputDir) {
        if (!StrUtil.isBlank(outputDir) && (new File(outputDir)).exists()) {
            try {
                openDir(outputDir);
            } catch (IOException var3) {
                log.error(var3.getMessage(), var3);
                showWindowAlert(var3.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            showWindowAlert("未找到输出目录：" + outputDir, Alert.AlertType.ERROR);
        }

    }

    @FXML
    public void choose(ActionEvent event) {

        final Window window = ((Node) event.getTarget()).getScene().getWindow();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        final File file = directoryChooser.showDialog(window);

        if (file != null) {
            tfProjectPath.setText(file.getAbsolutePath());
        }
    }


    /**
     * 保存配置
     */
    @FXML
    public void clear() {
        tfUrl.clear();
        tfUser.clear();
        pfPassword.clear();
        tfAuthor.clear();
        tfEntityPackage.clear();
        tfServicePackage.clear();
        tfProjectPath.clear();
        taTables.clear();
        taDomains.clear();
    }

    void showWindowAlert(String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle("Tip");
        alert.setContentText(message);
        alert.showAndWait();
    }


    @FXML
    private void initialize() {
        tfAuthor.setText(System.getProperty("user.name"));

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try (PrintWriter out = new PrintWriter(new java.io.FileWriter("log.log", true))) {
                throwable.printStackTrace(out);
            } catch (IOException exception) {
                showWindowAlert(exception.toString(), Alert.AlertType.ERROR);
            }
        });

        taTables.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                if (StrUtil.isBlank(taDomains.getText())) {
                    final String[] tables = taTables.getText().split(";");
                    List<String> converts = new ArrayList<>(tables.length + 1);
                    for (String table : tables) {
                        converts.add(StrUtil.upperFirst(StrUtil.toCamelCase(table, '_')));
                    }
                    taDomains.setText(StrUtil.join(";", converts));
                }
            }
        });

        final String absolutePath = new File(".").getAbsolutePath();

        File file = new File(absolutePath + File.separatorChar + JOTO_CONFIG);
        if (!file.exists()) {
            return;
        }

        FileReader fileReader = new FileReader(file.getAbsolutePath());
        final String jsonStr = fileReader.readString();

        if (StrUtil.isBlank(jsonStr)) {
            return;
        }

        final MyBatisGeneratorConfig config = JSONUtil.toBean(jsonStr, MyBatisGeneratorConfig.class);

        tfUrl.setText(config.getMysqlUrl());
        tfUser.setText(config.getMysqlUser());
        pfPassword.setText(config.getMysqlPwd());
        tfAuthor.setText(config.getAuthor());
        tfEntityPackage.setText(config.getEntityTargetPackage());
        tfServicePackage.setText(config.getServiceTargetPackage());
        tfProjectPath.setText(config.getTargetProject());
        taTables.setText(config.getTables());
        taDomains.setText(config.getDomains());
    }

}
