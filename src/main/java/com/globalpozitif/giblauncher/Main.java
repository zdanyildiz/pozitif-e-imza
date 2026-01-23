package com.globalpozitif.giblauncher;

import com.globalpozitif.giblauncher.core.JnlpParser;
import com.globalpozitif.giblauncher.core.model.Jar;
import com.globalpozitif.giblauncher.core.model.JnlpDoc;
import com.globalpozitif.giblauncher.core.service.ProcessManager;
import com.globalpozitif.giblauncher.core.service.ResourceDownloader;
import com.globalpozitif.giblauncher.ui.LoginView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) {
        // Start with Login Screen
        LoginView loginView = new LoginView(loginResponse -> {
            logger.info("User logged in: {}", loginResponse.getUserInfo().getEmail());
            showMainLoader(primaryStage);
        });
        loginView.show(primaryStage);
    }

    private void showMainLoader(Stage primaryStage) {
        Label statusLabel = new Label("Hazırlanıyor...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");

        ProgressBar progressBar = new ProgressBar(0.0);
        progressBar.setPrefWidth(350);
        progressBar.setStyle("-fx-accent: #2196F3;");

        VBox vBox = new VBox(15, statusLabel, progressBar);
        vBox.setAlignment(Pos.CENTER);
        vBox.setPadding(new Insets(20));

        StackPane root = new StackPane(vBox);
        root.setStyle("-fx-background-color: white;");

        primaryStage.setTitle("Pozitif E-İmza Başlatıcı");
        primaryStage.setScene(new Scene(root, 450, 200));
        primaryStage.centerOnScreen(); // Re-center after resizing

        startOrchestration(statusLabel, progressBar);
    }

    private void startOrchestration(Label statusLabel, ProgressBar progressBar) {
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

                        // URL building logic
                        String downloadUrl = href;
                        if (!downloadUrl.startsWith("http")) {
                            String base = codebase != null ? codebase : "";
                            if (!base.endsWith("/") && !downloadUrl.startsWith("/")) {
                                base += "/";
                            }
                            downloadUrl = base + downloadUrl;
                        }

                        // Local path logic
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
                pb.start();

                // Step E (Finish)
                Platform.runLater(Platform::exit);

                return null;
            }
        };

        // Binding
        statusLabel.textProperty().bind(task.messageProperty());
        progressBar.progressProperty().bind(task.progressProperty());

        // Error Handling
        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            logger.error("Hata oluştu: ", ex);

            // Unbind to set manual text
            statusLabel.textProperty().unbind();
            progressBar.progressProperty().unbind();

            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("Hata: " + (ex != null ? ex.getMessage() : "Bilinmeyen hata"));
            progressBar.setProgress(0);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
