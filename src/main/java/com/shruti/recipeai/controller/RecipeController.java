package com.shruti.recipeai.controller;

import com.shruti.recipeai.dto.ApiResponse;
import com.shruti.recipeai.dto.request.RecipeSuggestionRequestDto;
import com.shruti.recipeai.dto.response.RecipeSuggestionResponseDto;
import com.shruti.recipeai.security.CustomUserDetails;
import com.shruti.recipeai.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @PostMapping("/suggest")
    private ResponseEntity<ApiResponse<RecipeSuggestionResponseDto>> suggestRecipe(
            @Valid @RequestBody RecipeSuggestionRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        Long userId = userDetails.getId();
        RecipeSuggestionResponseDto response = recipeService.suggest(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Recipes fetched successfully", response));
    }
}
