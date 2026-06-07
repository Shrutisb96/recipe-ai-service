package com.shruti.recipeai.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shruti.recipeai.config.AiConfig;
import com.shruti.recipeai.dto.RecipeDto;
import com.shruti.recipeai.dto.request.RecipeSuggestionRequestDto;
import com.shruti.recipeai.exceptions.AiServiceException;
import com.shruti.recipeai.service.FallbackService;
import com.shruti.recipeai.service.ai.prompt.RecipePromptBuilder;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service("gemini")
@Slf4j
public class GeminiAiGatewayService implements AiGatewayService{

    private final RestTemplate restTemplate;
    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper;
    private final FallbackService fallbackService;
    private final RecipePromptBuilder promptBuilder;

    public GeminiAiGatewayService(
            @Qualifier("geminiRestTemplate") RestTemplate restTemplate,
            AiConfig aiConfig,
            ObjectMapper objectMapper,
            FallbackService fallbackService,
            RecipePromptBuilder promptBuilder) {
        this.restTemplate = restTemplate;
        this.aiConfig = aiConfig;
        this.objectMapper = objectMapper;
        this.fallbackService = fallbackService;
        this.promptBuilder = promptBuilder;
    }

    @Override
    @Retry(name = "ai")
    @CircuitBreaker(name = "ai", fallbackMethod = "handleFallback")
    public List<RecipeDto> suggest(RecipeSuggestionRequestDto request, List<String> normalised) {
        String prompt = promptBuilder.build(request, normalised);
        String url = aiConfig.getGemini().getApiUrl() + "?key=" + aiConfig.getGemini().getApiKey();

        Map<String, Object> requestBody = buildRequestBody(prompt);
        log.debug("GeminiAiGatewayService::Sending request to Gemini, prompt length: {}",
                prompt.length());

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    requestBody,
                    Map.class
            );
            return parseResponse(response.getBody());

        } catch (HttpClientErrorException e) {
            log.error("GeminiAiGatewayService::Gemini client error: {} {}",
                    e.getStatusCode(), e.getMessage());
            throw new AiServiceException("AI request failed: " + e.getMessage());
        } catch (HttpServerErrorException e) {
            log.error("GeminiAiGatewayService::Gemini server error: {} {}",
                    e.getStatusCode(), e.getMessage());
            throw new AiServiceException("AI service unavailable: " + e.getMessage());
        }

    }

    public List<RecipeDto> handleFallback(RecipeSuggestionRequestDto request,
                                          List<String> normalised,
                                          Throwable t) {
        log.warn("Gemini unavailable — serving fallback recipes. Reason: {}", t.getMessage());
        return fallbackService.getByPreference(request, normalised);
    }

    // build request
    private Map<String, Object> buildRequestBody(String prompt) {
        return Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", aiConfig.getGemini().getTemperature(),
                        "maxOutputTokens", aiConfig.getGemini().getMaxTokens(),
                        "responseMimeType", "application/json"
                )
        );
    }

    // Response parsing in JSON
    @SuppressWarnings("unchecked")
    private List<RecipeDto> parseResponse(Map responseBody) {
        try {
            if (responseBody == null) {
                log.error("GeminiAiGatewayService::Empty response from Gemini");
                throw new AiServiceException("GeminiAiGatewayService::Empty response from Gemini");
            }

            List<Map> candidates = (List<Map>) responseBody.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                log.error("GeminiAiGatewayService::No candidates in Gemini response");
                throw new AiServiceException("GeminiAiGatewayService::No candidates in Gemini response");
            }

            Map content = (Map) candidates.getFirst().get("content");
            List<Map> parts = (List<Map>) content.get("parts");
            String json = (String) parts.getFirst().get("text");

            log.debug("GeminiAiGatewayService::Raw Gemini response: {}", json);

            List<RecipeDto> recipes = objectMapper.readValue(
                    json,
                    new TypeReference<List<RecipeDto>>() {}
            );
            if (recipes == null || recipes.isEmpty()) {
                log.error("GeminiAiGatewayService::Gemini returned empty recipe list");
                throw new AiServiceException("Gemini returned empty recipe list");
            }

            log.info("GeminiAiGatewayService::Parsed {} recipes from Gemini", recipes.size());
            return recipes;

        } catch (JsonProcessingException e) {
            log.error("GeminiAiGatewayService::Failed to parse Gemini response: {}", e.getMessage());
            throw new AiServiceException("Failed to parse AI response");
        } catch (ClassCastException | NullPointerException e) {
            log.error("GeminiAiGatewayService::Unexpected Gemini response structure: {}", e.getMessage());
            throw new AiServiceException("Unexpected AI response structure");
        }
    }
}
