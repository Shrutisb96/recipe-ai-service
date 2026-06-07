package com.shruti.recipeai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;


@Configuration
@RequiredArgsConstructor
public class AppConfig {

    private final AiConfig aiConfig;

    @Bean("geminiRestTemplate")
    public RestTemplate geminiRestTemplate() {
        return buildRestTemplate(aiConfig.getGemini());
    }

    @Bean("claudeRestTemplate")
    public RestTemplate claudeRestTemplate() {
        return buildRestTemplate(aiConfig.getClaude());
    }

    private RestTemplate buildRestTemplate(AiConfig.ProviderConfig config) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getConnectTimeoutSeconds() * 1000);
        factory.setReadTimeout(config.getReadTimeoutSeconds() * 1000);
        return new RestTemplate(factory);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public TaskExecutor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-log-");
        executor.initialize();
        return executor;
    }
}
