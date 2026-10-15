package com.globalpozitif.giblauncher;

import com.globalpozitif.giblauncher.core.JnlpParser;
import com.globalpozitif.giblauncher.core.model.Jar;
import com.globalpozitif.giblauncher.core.model.JnlpDoc;
import com.globalpozitif.giblauncher.core.model.LoginResponse;
import com.globalpozitif.giblauncher.core.service.AuthService;
import com.globalpozitif.giblauncher.core.service.CredentialManager;
import com.globalpozitif.giblauncher.core.service.ProcessManager;
import com.globalpozitif.giblauncher.core.service.ResourceDownloader;
import com.globalpozitif.giblauncher.ui.LoginView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.awt.Desktop;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import javafx.scene.image.Image;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javafx.stage.FileChooser;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String DEFAULT_GIB_JNLP_URL = "https://ebelge.gib.gov.tr/EFaturaWebSocket/EFaturaWebSocket.jnlp";
    private String jnlpInputPath;

    @Override
    public void start(Stage primaryStage) {
        // Handle command line arguments for JNLP
        List<String> rawArgs = getParameters().getRaw();
        if (rawArgs != null && !rawArgs.isEmpty() && !rawArgs.get(0).trim().isEmpty()) {
            jnlpInputPath = rawArgs.get(0).trim();
        } else {
            File autoDetected = findLatestJnlpFile();
            if (autoDetected != null) {
                jnlpInputPath = autoDetected.getAbsolutePath();
                logger.info("Otomatik JNLP dosyası bulundu: {}", jnlpInputPath);
            } else {
                jnlpInputPath = promptUserForJnlp(primaryStage);
                if (jnlpInputPath == null || jnlpInputPath.isEmpty()) {
                    logger.warn("JNLP dosyası seçilmedi, uygulama kapatılıyor.");
                    Platform.exit();
                    return;
                }
            }
        }
        logger.info("JNLP Giriş Yolu: {}", jnlpInputPath);

        // Set Application Icon
        try {
            InputStream iconStream = getClass().getResourceAsStream("/images/appicon.png");
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            } else {
                logger.warn("Pencere ikonu bulunamadı: /images/appicon.png");
            }
        } catch (Exception e) {
            logger.warn("Pencere ikonu yuklenemedi: {}", e.getMessage());
        }

        // Otomatik giriş kontrolü
        CredentialManager credManager = new CredentialManager();
        String[] savedCreds = credManager.loadCredentials();

        if (savedCreds != null) {
            String email = savedCreds[0];
            String password = savedCreds[1];

            logger.info("Otomatik giriş deneniyor: {}", email);

            AuthService authService = new AuthService();
            Task<LoginResponse> autoLoginTask = new Task<>() {
                @Override
                protected LoginResponse call() throws Exception {
                    return authService.login(email, password);
                }
            };

            autoLoginTask.setOnSucceeded(e -> {
                LoginResponse response = autoLoginTask.getValue();
                if ("success".equals(response.getStatus())) {
                    logger.info("Otomatik giriş başarılı.");
                    showMainLoader(primaryStage, jnlpInputPath);
                } else {
                    logger.warn("Otomatik giriş başarısız: {}. Login ekranına yönlendiriliyor.", response.getMessage());
                    credManager.deleteCredentials();
                    showLogin(primaryStage, jnlpInputPath);
                }
            });

            autoLoginTask.setOnFailed(e -> {
                logger.error("Otomatik giriş sırasında hata oluştu. Login ekranına geçiliyor.",
                        autoLoginTask.getException());
                showLogin(primaryStage, jnlpInputPath);
            });

            new Thread(autoLoginTask).start();
        } else {
            showLogin(primaryStage, jnlpInputPath);
        }
    }

    private void showLogin(Stage stage, String jnlpPath) {
        LoginView loginView = new LoginView(loginResponse -> {
            logger.info("User logged in: {}", loginResponse.getUserInfo().getEmail());
            showMainLoader(stage, jnlpPath);
        });
        loginView.show(stage);
    }

    private void showMainLoader(Stage primaryStage, String jnlpPath) {
        // --- Logo Section ---
        ImageView logoView = new ImageView();
        try {
            InputStream logoStream = getClass().getResourceAsStream("/images/logo.png");
            if (logoStream != null) {
                Image image = new Image(logoStream);
                logoView.setImage(image);
                logoView.setFitWidth(150);
                logoView.setPreserveRatio(true);
            }
        } catch (Exception e) {
            logger.warn("Logo yuklenemedi: {}", e.getMessage());
        }

        Label statusLabel = new Label("Hazırlanıyor...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");

        ProgressBar progressBar = new ProgressBar(0.0);
        progressBar.setPrefWidth(350);
        progressBar.setStyle("-fx-accent: #2196F3;");

        VBox vBox = new VBox(15, logoView, statusLabel, progressBar);
        vBox.setAlignment(Pos.CENTER);
        vBox.setPadding(new Insets(30));

        StackPane root = new StackPane(vBox);
        root.setStyle("-fx-background-color: white;");

        primaryStage.setTitle("Pozitif E-İmza Başlatıcı");
        primaryStage.setScene(new Scene(root, 450, 300));
        primaryStage.centerOnScreen();

        startOrchestration(statusLabel, progressBar, vBox, jnlpPath);
    }

    private void startOrchestration(Label statusLabel, ProgressBar progressBar, VBox vBox, String jnlpPath) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Step A (Init)
                String cacheDir = System.getProperty("user.home") + File.separator + ".giblauncher" + File.separator
                        + "cache";
                Path cachePath = Paths.get(cacheDir);

                // Step B (Parse)
                updateMessage("Yapılandırma okunuyor...");
                updateProgress(0.1, 1.0);
                JnlpParser parser = new JnlpParser();
                JnlpDoc jnlpDoc;
                if (jnlpPath.startsWith("http://") || jnlpPath.startsWith("https://")) {
                    jnlpDoc = parser.parse(new URL(jnlpPath));
                } else {
                    jnlpDoc = parser.parse(new File(jnlpPath));
                }

                // Step C (Download)
                updateMessage("Dosyalar indiriliyor...");
                ResourceDownloader downloader = new ResourceDownloader();

                if (jnlpDoc.getResources() != null && jnlpDoc.getResources().getJars() != null) {
                    List<Jar> jars = jnlpDoc.getResources().getJars();
                    int total = jars.size();
                    String codebase = jnlpDoc.getCodebase();

                    for (int i = 0; i < total; i++) {
                        Jar jar = jars.get(i);
                        String href = jar.getHref();
                        String downloadUrl = href;
                        if (!downloadUrl.startsWith("http")) {
                            String base = codebase != null ? codebase : "";
                            if (!base.endsWith("/") && !downloadUrl.startsWith("/"))
                                base += "/";
                            downloadUrl = base + downloadUrl;
                        }
                        String fileName = href.contains("/") ? href.substring(href.lastIndexOf("/") + 1) : href;
                        Path destination = cachePath.resolve(fileName);
                        downloader.downloadFile(downloadUrl, destination);
                        updateProgress(i + 1, total);
                    }
                }

                // Step D (Launch)
                updateMessage("Uygulama başlatılıyor...");
                ProcessManager processManager = new ProcessManager();
                ProcessBuilder pb = processManager.buildProcess(jnlpDoc, cachePath);
                logger.info("Dış uygulama başlatılıyor...");
                Process process = pb.start();

                // Sleep yerine process'in durumunu izle ve UI'yı güncelle
                updateMessage("Uygulama çalışıyor...");
                updateProgress(1.0, 1.0);

                // Alt sürecin sonlanmasını bekle
                int exitCode = process.waitFor();

                if (exitCode != 0) {
                    throw new Exception("Uygulama beklenmeyen bir hata koduyla sonlandı (Exit Code: " + exitCode + ")");
                }

                logger.info("Dış uygulama başarıyla sonlandı, launcher kapatılıyor.");
                Platform.runLater(Platform::exit);

                return null;
            }
        };

        statusLabel.textProperty().bind(task.messageProperty());
        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            logger.error("Hata oluştu: ", ex);
            statusLabel.textProperty().unbind();
            progressBar.progressProperty().unbind();
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("Hata: " + (ex != null ? ex.getMessage() : "Bilinmeyen hata"));
            progressBar.setProgress(0);

            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER);
            Button openLogBtn = new Button("Log Dosyasını Aç");
            openLogBtn.setOnAction(e -> {
                try {
                    File logFile = new File(System.getProperty("user.home") + "/.giblauncher/logs/launcher.log");
                    if (logFile.exists() && Desktop.isDesktopSupported())
                        Desktop.getDesktop().open(logFile);
                } catch (Exception ex2) {
                    logger.error("Log dosyasi acilamadi", ex2);
                }
            });
            Button closeBtn = new Button("Kapat");
            closeBtn.setOnAction(e -> Platform.exit());
            buttonBox.getChildren().addAll(openLogBtn, closeBtn);
            vBox.getChildren().add(buttonBox);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private File findLatestJnlpFile() {
        List<File> searchDirs = new ArrayList<>();
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isEmpty()) {
            searchDirs.add(new File(userHome, "Downloads"));
        }
        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isEmpty()) {
            searchDirs.add(new File(userDir));
        }

        List<File> efaturaFiles = new ArrayList<>();
        List<File> anyJnlpFiles = new ArrayList<>();

        for (File dir : searchDirs) {
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles((d, name) -> name != null && name.toLowerCase(Locale.ROOT).endsWith(".jnlp"));
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile()) {
                            String lowerName = f.getName().toLowerCase(Locale.ROOT);
                            if (lowerName.startsWith("efaturawebsocket")) {
                                efaturaFiles.add(f);
                            } else {
                                anyJnlpFiles.add(f);
                            }
                        }
                    }
                }
            }
        }

        if (!efaturaFiles.isEmpty()) {
            efaturaFiles.sort(Comparator.comparingLong(File::lastModified).reversed());
            return efaturaFiles.get(0);
        }
        if (!anyJnlpFiles.isEmpty()) {
            anyJnlpFiles.sort(Comparator.comparingLong(File::lastModified).reversed());
            return anyJnlpFiles.get(0);
        }
        return null;
    }

    private String promptUserForJnlp(Stage primaryStage) {
        try {
            UIManager.put("FileChooser.openDialogTitleText", "JNLP Dosyası Seçin");
            UIManager.put("FileChooser.lookInLabelText", "Konum:");
            UIManager.put("FileChooser.openButtonText", "Aç");
            UIManager.put("FileChooser.cancelButtonText", "İptal");
            UIManager.put("FileChooser.fileNameLabelText", "Dosya Adı:");
            UIManager.put("FileChooser.filesOfTypeLabelText", "Dosya Türü:");
            UIManager.put("FileChooser.openButtonToolTipText", "Seçili JNLP dosyasını aç");
            UIManager.put("FileChooser.cancelButtonToolTipText", "İptal et");

            Object[] options = { "Bilgisayardan JNLP Seç", "GİB Varsayılan JNLP İndir", "İptal" };
            int choice = JOptionPane.showOptionDialog(
                    null,
                    "İndirilenler klasöründe otomatik JNLP dosyası bulunamadı.\nLütfen bilgisayarınızdan bir JNLP dosyası seçin veya GİB varsayılan JNLP bağlantısını kullanın.",
                    "Pozitif E-İmza Başlatıcı - JNLP Seçimi",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (choice == 1) {
                return DEFAULT_GIB_JNLP_URL;
            } else if (choice != 0) {
                return null;
            }

            JFileChooser jFileChooser = new JFileChooser();
            jFileChooser.setDialogTitle("JNLP Dosyası Seçin");
            jFileChooser.setFileFilter(new FileNameExtensionFilter("JNLP Dosyaları (*.jnlp)", "jnlp"));
            jFileChooser.setAcceptAllFileFilterUsed(false);
            File downloadsDir = new File(System.getProperty("user.home", "."), "Downloads");
            if (downloadsDir.exists() && downloadsDir.isDirectory()) {
                jFileChooser.setCurrentDirectory(downloadsDir);
            }

            int result = jFileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION && jFileChooser.getSelectedFile() != null) {
                return jFileChooser.getSelectedFile().getAbsolutePath();
            }
            return null;
        } catch (Exception e) {
            logger.warn("JFileChooser açılamadı, JavaFX FileChooser deneniyor: {}", e.getMessage());
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("JNLP Dosyası Seçin");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JNLP Dosyaları (*.jnlp)", "*.jnlp"));
            File downloadsDir = new File(System.getProperty("user.home", "."), "Downloads");
            if (downloadsDir.exists() && downloadsDir.isDirectory()) {
                fileChooser.setInitialDirectory(downloadsDir);
            }
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            return selectedFile != null ? selectedFile.getAbsolutePath() : null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
