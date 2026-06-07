package com.shruti.recipeai.service;

import com.shruti.recipeai.common.CacheKeyGenerator;
import com.shruti.recipeai.constants.CachePrefix;
import com.shruti.recipeai.constants.CacheTtl;
import com.shruti.recipeai.dto.RecipeDto;
import com.shruti.recipeai.dto.request.RecipeSuggestionRequestDto;
import com.shruti.recipeai.dto.response.RecipeSuggestionResponseDto;
import com.shruti.recipeai.service.ai.AiGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeService {

    private final CacheKeyGenerator cacheKeyGenerator;
    private final CacheService cacheService;
    private final SuggestionLogService suggestionLogService;
    private final AiGatewayService aiGatewayService;

    // TODO v2 — extract corrected ingredients from AI response and use those
    // for cache key, suggestion_logs, and ingredient_hash instead of raw normalised.
    // Also surface unrecognised ingredients in warnings[].
    public RecipeSuggestionResponseDto suggest(RecipeSuggestionRequestDto request, Long userId) {

        // Normalize the ingredient list
        List<String> normalisedIngredientList = request.getIngredients().stream()
                .map(String::toLowerCase)
                .map(String::trim)
                .sorted()
                .toList();

        // Generate Cache key
        String cacheKey = cacheKeyGenerator.generateRecipeSuggestionKey(
                normalisedIngredientList, request.getTargetCalories(), request.getDietaryPreference());

        // Check Redis Cache for recipes
        List<RecipeDto> cached = cacheService.get(CachePrefix.RECIPES, cacheKey);
        if (cached != null) {
            log.info("Cache hit for key: {}", cacheKey);
            suggestionLogService.log(userId, normalisedIngredientList, request, true);
            return buildResponse(cached, request.getPage(), true);
        }

        // Cache hit miss so call AI client
        log.info("Cache miss — calling AI for userId: {}", userId);
        List<RecipeDto> recipes = aiGatewayService.suggest(request, normalisedIngredientList);

        // save in cache
        cacheService.save(CachePrefix.RECIPES, cacheKey, recipes, CacheTtl.DEFAULT);

        // Add to suggestion logs db
        suggestionLogService.log(userId, normalisedIngredientList, request, false);

        // build response
        return buildResponse(recipes, request.getPage(), false);
    }

    private RecipeSuggestionResponseDto buildResponse(List<RecipeDto> recipes,
                                                int page, boolean cacheHit) {
        //TODO move this to env variable
        int pageSize = 5;
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, recipes.size());

        List<RecipeDto> pageRecipes = (fromIndex < recipes.size())
                ? recipes.subList(fromIndex, toIndex)
                : List.of();

        return RecipeSuggestionResponseDto.builder()
                .recipes(pageRecipes)
                .page(page)
                .totalPages((int) Math.ceil((double) recipes.size() / pageSize))
                .cacheHit(cacheHit)
                .warnings(List.of())
                .build();
    }
}
