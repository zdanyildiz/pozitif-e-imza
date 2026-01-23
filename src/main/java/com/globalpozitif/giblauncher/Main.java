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
import java.util.List;

public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) {
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
                    showMainLoader(primaryStage);
                } else {
                    logger.warn("Otomatik giriş başarısız: {}. Login ekranına yönlendiriliyor.", response.getMessage());
                    credManager.deleteCredentials();
                    showLogin(primaryStage);
                }
            });

            autoLoginTask.setOnFailed(e -> {
                logger.error("Otomatik giriş sırasında hata oluştu. Login ekranına geçiliyor.",
                        autoLoginTask.getException());
                showLogin(primaryStage);
            });

            new Thread(autoLoginTask).start();
        } else {
            showLogin(primaryStage);
        }
    }

    private void showLogin(Stage stage) {
        LoginView loginView = new LoginView(loginResponse -> {
            logger.info("User logged in: {}", loginResponse.getUserInfo().getEmail());
            showMainLoader(stage);
        });
        loginView.show(stage);
    }

    private void showMainLoader(Stage primaryStage) {
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

        startOrchestration(statusLabel, progressBar, vBox);
    }

    private void startOrchestration(Label statusLabel, ProgressBar progressBar, VBox vBox) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Step A (Init)
                String jnlpUrl = "https://ebelge.gib.gov.tr/EFaturaWebSocket/EFaturaWebSocket.jnlp";
                String cacheDir = System.getProperty("user.home") + File.separator + ".giblauncher" + File.separator
                        + "cache";
                Path cachePath = Paths.get(cacheDir);

                // Step B (Parse)
                updateMessage("Yapılandırma okunuyor...");
                updateProgress(0.1, 1.0);
                JnlpParser parser = new JnlpParser();
                JnlpDoc jnlpDoc = parser.parse(new URL(jnlpUrl));

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
                updateProgress(1.0, 1.0);
                ProcessManager processManager = new ProcessManager();
                ProcessBuilder pb = processManager.buildProcess(jnlpDoc, cachePath);
                logger.info("Dış uygulama başlatılıyor...");
                Process process = pb.start();
                Thread.sleep(3000);

                if (!process.isAlive()) {
                    int exitCode = process.exitValue();
                    throw new Exception("Uygulama başlatılamadı (Exit Code: " + exitCode + ")");
                }

                logger.info("Dış uygulama başarıyla başlatıldı, launcher kapatılıyor.");
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

    public static void main(String[] args) {
        launch(args);
    }
}
