package com.globalpozitif.giblauncher.ui;

import com.globalpozitif.giblauncher.core.model.LoginResponse;
import com.globalpozitif.giblauncher.core.service.AuthService;
import com.globalpozitif.giblauncher.core.service.CredentialManager;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.InputStream;
import java.net.URI;
import java.util.function.Consumer;

public class LoginView {

    private final AuthService authService;
    private final Consumer<LoginResponse> onSuccess;

    public LoginView(Consumer<LoginResponse> onSuccess) {
        this.authService = new AuthService();
        this.onSuccess = onSuccess;
    }

    public void show(Stage stage) {
        // --- Logo Section ---
        ImageView logoView = new ImageView();
        try {
            // Logo dosyasını resources/images/logo.png yolundan yüklemeye çalışıyoruz
            InputStream logoStream = getClass().getResourceAsStream("/images/logo.png");
            if (logoStream != null) {
                Image image = new Image(logoStream);
                logoView.setImage(image);
                logoView.setFitWidth(200); // Genişliği sınırla
                logoView.setPreserveRatio(true);
            }
        } catch (Exception e) {
            // Logo yüklenemezse sessizce geç, sadece title görünür
            System.err.println("Logo yüklenemedi: " + e.getMessage());
        }

        Label titleLabel = new Label("Pozitif E-İmza Giriş");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #333;");

        TextField emailField = new TextField();
        emailField.setPromptText("E-Posta Adresi");
        emailField.setPrefWidth(250);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Şifre");
        passwordField.setPrefWidth(250);

        Button loginButton = new Button("Giriş Yap");
        loginButton.setStyle(
                "-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");
        loginButton.setPrefWidth(250);

        // --- Duyurular Linki ---
        Hyperlink announcementsLink = new Hyperlink("Duyurular ve Yardım (e-imza.globalpozitif.com.tr)");
        announcementsLink.setStyle("-fx-font-size: 12px;");
        announcementsLink.setOnAction(e -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("https://e-imza.globalpozitif.com.tr"));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Label messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(250);

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setMaxHeight(20);

        loginButton.setOnAction(e -> {
            String email = emailField.getText();
            String password = passwordField.getText();

            if (email.isEmpty() || password.isEmpty()) {
                messageLabel.setTextFill(Color.RED);
                messageLabel.setText("Lütfen tüm alanları doldurun.");
                return;
            }

            // Disable controls
            loginButton.setDisable(true);
            emailField.setDisable(true);
            passwordField.setDisable(true);
            progressIndicator.setVisible(true);
            messageLabel.setText("");

            Task<LoginResponse> loginTask = new Task<>() {
                @Override
                protected LoginResponse call() throws Exception {
                    return authService.login(email, password);
                }
            };

            loginTask.setOnSucceeded(event -> {
                LoginResponse response = loginTask.getValue();
                progressIndicator.setVisible(false);
                loginButton.setDisable(false);
                emailField.setDisable(false);
                passwordField.setDisable(false);

                if ("success".equals(response.getStatus())) {
                    // Beni hatırla özelliği: Bilgileri şifreli olarak kaydet
                    CredentialManager credManager = new CredentialManager();
                    credManager.saveCredentials(email, password);

                    messageLabel.setTextFill(Color.GREEN);
                    messageLabel.setText("Giriş başarılı! Yönlendiriliyorsunuz...");
                    onSuccess.accept(response);
                } else {
                    messageLabel.setTextFill(Color.RED);
                    messageLabel.setText(response.getMessage() != null ? response.getMessage() : "Giriş başarısız.");
                }
            });

            loginTask.setOnFailed(event -> {
                progressIndicator.setVisible(false);
                loginButton.setDisable(false);
                emailField.setDisable(false);
                passwordField.setDisable(false);
                messageLabel.setTextFill(Color.RED);
                Throwable ex = loginTask.getException();
                messageLabel.setText("Hata: " + ex.getMessage());
            });

            new Thread(loginTask).start();
        });

        // VBox sıralamasını güncelle: Logo en üstte, Link en altta
        VBox root = new VBox(15, logoView, titleLabel, emailField, passwordField, loginButton, announcementsLink,
                progressIndicator, messageLabel);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: white;");

        Scene scene = new Scene(root, 400, 450); // Yükseklik biraz artırıldı
        stage.setScene(scene);
        stage.setTitle("Giriş Yap - Pozitif E-İmza");
        stage.setResizable(false);
        stage.show();
    }
}
