package com.shruti.recipeai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "ai")
@Component
@Data
public class AiConfig {

    private String provider;
    private ProviderConfig gemini = new ProviderConfig();
    private ProviderConfig claude = new ProviderConfig();

    @Data
    public static class ProviderConfig {
        private String apiUrl;
        private String apiKey;
        private int maxTokens;
        private double temperature;
        private int connectTimeoutSeconds;
        private int readTimeoutSeconds;
    }

    // Convenience method — returns active provider config
    public ProviderConfig getActiveProvider() {
        return switch (provider.toLowerCase()) {
            case "gemini" -> gemini;
            case "claude" -> claude;
            default -> throw new IllegalArgumentException(
                    "Unknown ai provider: " + provider
            );
        };
    }
}
