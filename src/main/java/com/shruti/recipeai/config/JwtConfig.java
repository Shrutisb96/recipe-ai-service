package com.shruti.recipeai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data  // Lombok
public class JwtConfig {
    private String secret;
    private int expiryHours;
}
