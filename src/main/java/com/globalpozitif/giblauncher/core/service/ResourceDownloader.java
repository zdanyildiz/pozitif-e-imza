package com.globalpozitif.giblauncher.core.service;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ResourceDownloader {
    private static final Logger logger = LoggerFactory.getLogger(ResourceDownloader.class);
    private static final String ALLOWED_BASE_URL = "https://ebelge.gib.gov.tr/";
    private static final int TIMEOUT_SECONDS = 30;

    /**
     * Güvenlik kontrolü yapar. URL kesinlikle ALLOWED_BASE_URL ile başlamalıdır.
     *
     * @param fullUrl Kontrol edilecek tam URL
     * @throws SecurityException URL güvenli değilse fırlatılır
     */
    public void validateUrl(String fullUrl) {
        if (fullUrl == null || !fullUrl.startsWith(ALLOWED_BASE_URL)) {
            logger.error("Güvenlik ihlali: Geçersiz URL - {}", fullUrl);
            throw new SecurityException("URL kesinlikle " + ALLOWED_BASE_URL + " ile başlamalıdır.");
        }
    }

    /**
     * Belirtilen URL'deki dosyayı hedef yola indirir.
     *
     * @param url         İndirilecek dosyanın URL'si
     * @param destination Dosyanın kaydedileceği yol
     * @throws IOException İndirme sırasında bir hata oluşursa fırlatılır
     */
    public void downloadFile(String url, Path destination) throws IOException {
        validateUrl(url);

        // Basit cache kontrolü
        if (Files.exists(destination) && Files.size(destination) > 0) {
            logger.info("Dosya zaten mevcut ve boyutu > 0, indirme atlanıyor: {}", destination);
            return;
        }

        logger.info("Dosya indiriliyor: {} -> {}", url, destination);

        // Parent dizini yoksa oluştur
        if (destination.getParent() != null) {
            Files.createDirectories(destination.getParent());
        }

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(TIMEOUT_SECONDS))
                .setResponseTimeout(Timeout.ofSeconds(TIMEOUT_SECONDS))
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            HttpGet httpGet = new HttpGet(url);

            httpClient.execute(httpGet, response -> {
                int status = response.getCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        try (InputStream is = entity.getContent()) {
                            Files.copy(is, destination, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } else {
                    throw new IOException("Sunucu hata döndürdü: " + status + " - " + response.getReasonPhrase());
                }
                return null;
            });
        }

        logger.info("İndirme başarıyla tamamlandı: {}", destination);
    }
}
