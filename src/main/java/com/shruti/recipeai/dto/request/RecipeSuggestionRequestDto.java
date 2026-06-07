package com.shruti.recipeai.dto.request;

import com.shruti.recipeai.constants.DietaryPreference;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class RecipeSuggestionRequestDto {

    @NotEmpty(message = "Ingredients must not be empty")
    private List<String> ingredients;

    @Min(value = 1, message = "targetCalories must be greater than 1")
    private Integer targetCalories;

    private DietaryPreference dietaryPreference = DietaryPreference.NONE;

    @Min(0) @Max(1)
    private int page = 0;
}
