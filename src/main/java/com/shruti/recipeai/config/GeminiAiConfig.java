package com.shruti.recipeai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai.gemini")
@Data
public class GeminiAiConfig {
    private String apiKey;
    private String apiUrl;
    private String model;
    private int maxTokens;
    private int timeoutSeconds;
    private double temperature;
}
