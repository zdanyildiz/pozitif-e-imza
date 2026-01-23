package com.globalpozitif.giblauncher.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * Kullanıcı kimlik bilgilerini yerel diskte şifreli olarak saklar ve yükler.
 */
public class CredentialManager {
    private static final Logger logger = LoggerFactory.getLogger(CredentialManager.class);
    private static final String FOLDER_NAME = "PozitifEImza";
    private static final String FILE_NAME = "encrypted_credentials.txt";
    private final SecurityService securityService = new SecurityService();

    private Path getFilePath() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null) {
            // Windows dışı sistemlerde veya LOCALAPPDATA yoksa fallback
            localAppData = System.getProperty("user.home") + File.separator + ".pozitif-e-imza";
        }
        return Paths.get(localAppData, FOLDER_NAME, FILE_NAME);
    }

    /**
     * Bilgileri E-posta|Şifre|BitişZamanı formatında şifreleyerek kaydeder.
     */
    public void saveCredentials(String email, String password) {
        try {
            Path path = getFilePath();
            Files.createDirectories(path.getParent());

            // 30 günlük oturum süresi
            String expiry = LocalDateTime.now().plusDays(30).toString();
            String data = email + "|" + password + "|" + expiry;

            String encryptedData = securityService.encrypt(data);
            if (encryptedData != null) {
                Files.writeString(path, encryptedData);
                logger.info("Credentials saved successfully to {}", path);
            }
        } catch (Exception e) {
            logger.error("Failed to save credentials", e);
        }
    }

    /**
     * Şifrelenmiş bilgileri çözer ve geçerliliğini (zaman bazlı) kontrol eder.
     */
    public String[] loadCredentials() {
        try {
            Path path = getFilePath();
            if (!Files.exists(path))
                return null;

            String encryptedData = Files.readString(path);
            String decryptedData = securityService.decrypt(encryptedData);

            if (decryptedData != null && decryptedData.contains("|")) {
                String[] parts = decryptedData.split("\\|");
                if (parts.length >= 3) {
                    LocalDateTime expiry = LocalDateTime.parse(parts[2]);
                    if (LocalDateTime.now().isBefore(expiry)) {
                        return parts; // [email, password, expiry]
                    } else {
                        logger.warn("Saved credentials have expired.");
                        deleteCredentials();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load credentials", e);
        }
        return null;
    }

    public void deleteCredentials() {
        try {
            Files.deleteIfExists(getFilePath());
            logger.info("Credentials deleted.");
        } catch (Exception e) {
            logger.error("Failed to delete credentials", e);
        }
    }
}
