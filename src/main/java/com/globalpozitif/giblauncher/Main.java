package com.globalpozitif.giblauncher;

import com.globalpozitif.giblauncher.core.JnlpParser;
import com.globalpozitif.giblauncher.core.model.Jar;
import com.globalpozitif.giblauncher.core.model.JnlpDoc;
import com.globalpozitif.giblauncher.core.service.ProcessManager;
import com.globalpozitif.giblauncher.core.service.ResourceDownloader;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String JNLP_URL = "https://ebelge.gib.gov.tr/EFaturaWebSocket/EFaturaWebSocket.jnlp";
    private static final String CACHE_DIR = "cache";

    private Label statusLabel;
    private ProgressBar progressBar;

    @Override
    public void start(Stage primaryStage) {
        setupUI(primaryStage);
        startLaunchTask();
    }

    private void setupUI(Stage primaryStage) {
        statusLabel = new Label("Hazırlanıyor...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(350);
        progressBar.setStyle("-fx-accent: #2196F3;");

        VBox root = new VBox(20, statusLabel, progressBar);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #f5f5f5;");

        Scene scene = new Scene(root, 450, 200);
        primaryStage.setTitle("GİB E-Fatura Uygulama Başlatıcı");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void startLaunchTask() {
        Task<Void> launchTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    updateMessage("JNLP yapılandırması alınıyor...");
                    updateProgress(0.1, 1.0);

                    JnlpParser parser = new JnlpParser();
                    JnlpDoc jnlpDoc = parser.parse(new URL(JNLP_URL));

                    updateMessage("Dosyalar kontrol ediliyor...");
                    updateProgress(0.2, 1.0);

                    ResourceDownloader downloader = new ResourceDownloader();
                    Path cachePath = Paths.get(CACHE_DIR);

                    if (jnlpDoc.getResources() == null || jnlpDoc.getResources().getJars() == null) {
                        throw new Exception("JNLP dosyasında JAR kaynakları bulunamadı.");
                    }

                    List<Jar> jars = jnlpDoc.getResources().getJars();
                    int totalJars = jars.size();
                    String codebase = jnlpDoc.getCodebase();

                    for (int i = 0; i < totalJars; i++) {
                        Jar jar = jars.get(i);
                        String href = jar.getHref();

                        // Tam URL oluşturma
                        String fileUrl = href;
                        if (!fileUrl.startsWith("http")) {
                            String base = codebase != null ? codebase : "";
                            if (!base.endsWith("/") && !fileUrl.startsWith("/")) {
                                base += "/";
                            }
                            fileUrl = base + fileUrl;
                        }

                        // Dosya adını temizleme (href path içerebilir)
                        String fileName = href;
                        if (fileName.contains("/")) {
                            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                        }
                        Path dest = cachePath.resolve(fileName);

                        updateMessage("İndiriliyor: " + fileName + " (" + (i + 1) + "/" + totalJars + ")");
                        downloader.downloadFile(fileUrl, dest);

                        double progress = 0.2 + (0.6 * (i + 1) / totalJars);
                        updateProgress(progress, 1.0);
                    }

                    updateMessage("Uygulama başlatılıyor...");
                    updateProgress(0.9, 1.0);

                    ProcessManager processManager = new ProcessManager();
                    ProcessBuilder pb = processManager.buildProcess(jnlpDoc, cachePath);
                    pb.start();

                    updateMessage("Uygulama başarıyla başlatıldı.");
                    updateProgress(1.0, 1.0);

                    // Başarılı başlatma sonrası kısa bir süre bekleyip kapatabiliriz
                    Thread.sleep(1000);

                } catch (Exception e) {
                    logger.error("Başlatma sırasında hata oluştu:", e);
                    throw e; // Task'ın failed() metoduna düşmesi için
                }
                return null;
            }

            @Override
            protected void succeeded() {
                logger.info("Launcher başarıyla görevini tamamladı.");
                Platform.exit();
                System.exit(0);
            }

            @Override
            protected void failed() {
                Throwable exception = getException();
                String errorMessage = exception != null ? exception.getMessage() : "Bilinmeyen bir hata oluştu.";

                Platform.runLater(() -> {
                    statusLabel.setTextFill(Color.RED);
                    statusLabel.setText("Hata: " + errorMessage);
                    progressBar.setProgress(0);
                    progressBar.setStyle("-fx-accent: red;");
                });
            }
        };

        // UI Binding
        statusLabel.textProperty().bind(launchTask.messageProperty());
        progressBar.progressProperty().bind(launchTask.progressProperty());

        Thread thread = new Thread(launchTask);
        thread.setDaemon(true);
        thread.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
