package com.shruti.recipeai.service.ai;

import com.shruti.recipeai.dto.RecipeDto;
import com.shruti.recipeai.dto.request.RecipeSuggestionRequestDto;

import java.util.List;

public interface AiGatewayService {

    List<RecipeDto> suggest(RecipeSuggestionRequestDto request,
                            List<String> normalised);
}
