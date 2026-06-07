package com.shruti.recipeai.dto.response;

import com.shruti.recipeai.dto.RecipeDto;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class RecipeSuggestionResponseDto {

    private List<RecipeDto> recipes;

    private int page;

    private int totalPages;

    private boolean cacheHit;

    List<String> warnings;
}
