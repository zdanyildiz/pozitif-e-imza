package com.globalpozitif.giblauncher.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalpozitif.giblauncher.core.model.LoginResponse;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.prefs.Preferences;

public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String API_URL = "https://eimza.globalpozitif.com.tr/?/webservice/member/post/checkLastOrderByEmailAndPassword";
    private static final String PREF_NODE_NAME = "com.globalpozitif.giblauncher";
    private static final String PREF_COMPUTER_ID_KEY = "computerId";

    private final ObjectMapper objectMapper;

    public AuthService() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Gets or generates a unique Computer ID that persists across runs.
     */
    public String getComputerId() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE_NAME);
        String computerId = prefs.get(PREF_COMPUTER_ID_KEY, null);

        if (computerId == null) {
            computerId = UUID.randomUUID().toString();
            prefs.put(PREF_COMPUTER_ID_KEY, computerId);
            try {
                prefs.flush();
            } catch (Exception e) {
                logger.error("Could not save Computer ID to preferences", e);
            }
        }
        return computerId;
    }

    /**
     * Authenticates the user with email and password.
     */
    public LoginResponse login(String email, String password) throws IOException {
        String computerId = getComputerId();

        Map<String, String> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("password", password); // Server side encryption is expected as per doc
        payload.put("computerId", computerId);

        String jsonPayload = objectMapper.writeValueAsString(payload);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))
                .setResponseTimeout(Timeout.ofSeconds(10))
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            HttpPost httpPost = new HttpPost(API_URL);
            httpPost.setEntity(new StringEntity(jsonPayload, ContentType.APPLICATION_JSON));

            return httpClient.execute(httpPost, response -> {
                int status = response.getCode();
                if (status >= 200 && status < 300) {
                    return objectMapper.readValue(response.getEntity().getContent(), LoginResponse.class);
                } else {
                    throw new IOException("API Error: " + status + " - " + response.getReasonPhrase());
                }
            });
        }
    }
}
