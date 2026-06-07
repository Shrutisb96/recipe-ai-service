package com.shruti.recipeai.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeDto {

    private String name;

    private List<String> ingredients;

    private List<String> missingIngredients;

    private List<String> steps;

    private Integer estimatedCalories;

    private Integer prepTimeMinutes;

    private List<String> tags;

    @JsonIgnore
    private boolean fallback = false;
}
