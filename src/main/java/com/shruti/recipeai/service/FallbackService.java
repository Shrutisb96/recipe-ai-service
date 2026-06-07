package com.shruti.recipeai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shruti.recipeai.common.CacheKeyGenerator;
import com.shruti.recipeai.constants.DietaryPreference;
import com.shruti.recipeai.dto.RecipeDto;
import com.shruti.recipeai.dto.request.RecipeSuggestionRequestDto;
import com.shruti.recipeai.entity.FallbackRecipe;
import com.shruti.recipeai.exceptions.AiServiceException;
import com.shruti.recipeai.repository.FallbackRecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FallbackService {

    private final FallbackRecipeRepository fallbackRecipeRepository;
    private final ObjectMapper objectMapper;
    private final CacheKeyGenerator cacheKeyGenerator;

    private static final int DEFAULT_LIMIT = 10;

    public List<RecipeDto> getByPreference(RecipeSuggestionRequestDto request,
                                           List<String> normalised) {

        try {
            // Create ingredient Hash and search DB
            String hash = cacheKeyGenerator.generateIngredientHash(normalised);
            List<FallbackRecipe> recipesByHash = fallbackRecipeRepository
                    .findByIngredientHash(hash, DEFAULT_LIMIT);

            if (!recipesByHash.isEmpty()) {
                log.info("Fallback: exact hash match recipes found");
                return toRecipeDtos(recipesByHash);
            }
            log.info("Fallback: no hash match - using dietary filter");

            // Search by dietary filter and search DB
            DietaryPreference filter = resolveFilter(request.getDietaryPreference());
            List<FallbackRecipe> recipesByDietaryFilter = fallbackRecipeRepository
                    .findByDietaryFilter(filter.name(), DEFAULT_LIMIT);

            if (!recipesByDietaryFilter.isEmpty())
                return toRecipeDtos(recipesByDietaryFilter);
            log.warn("Fallback: no dietary filter match - returning random recipes");

            // else return random recipes
            return toRecipeDtos(fallbackRecipeRepository.findRandom(DEFAULT_LIMIT));
        } catch (Exception e) {
            log.error("FallbackService:: Fetching fallback recipe failed: {}", e.getMessage());
            throw new AiServiceException("Fallback service also failed: " + e.getMessage());
        }
    }

    private DietaryPreference resolveFilter(DietaryPreference preference) {
        return (preference == null) ? DietaryPreference.NONE : preference;
    }

    private List<RecipeDto> toRecipeDtos(List<FallbackRecipe> recipes) {
        return recipes.stream()
                .map(this::toRecipeDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private RecipeDto toRecipeDto(FallbackRecipe fallbackRecipe) {
        try {
            RecipeDto dto = objectMapper.readValue(
                    fallbackRecipe.getRecipeJson(),
                    RecipeDto.class
            );
            dto.setFallback(true);
            return dto;
        } catch (JsonProcessingException e) {
            log.error("FallbackService:: Failed to parse fallback recipe id={} : {}",
                    fallbackRecipe.getId(), e.getMessage());
            return null;
        }
    }
}
