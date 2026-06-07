package com.shruti.recipeai.service.ai.prompt;

import com.shruti.recipeai.constants.DietaryPreference;
import com.shruti.recipeai.dto.request.RecipeSuggestionRequestDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RecipePromptBuilder {

    //TODO move to txt file later so it is configurable
    private static final String PROMPT_TEMPLATE = """
            You are a recipe suggestion assistant. Return EXACTLY 10 recipe
            suggestions as a JSON array.
            No markdown, no explanation, no extra text — only the raw JSON array.

            User has these ingredients: {ingredients}
            {calorieInstruction}
            Dietary preference: {preference}

            Each recipe object must follow this exact schema:
            {
              "name": "string",
              "ingredients": ["only ingredients the user already has"],
              "missingIngredients": ["items needed but user does not have"],
              "steps": ["step 1", "step 2"],
              "estimatedCalories": 123,
              "prepTimeMinutes": 20,
              "tags": ["tag1", "tag2", "tag3"]
            }

            Rules:

            1. ingredients
               Only include items from the user's available list above.
            2. missingIngredients
               Only include additional items the recipe needs that the user does NOT have.
            3. dietary preference
               All 10 recipes must strictly satisfy: {preference}
               If preference is "none", no filtering is applied.
            4. name
               Use the most common, universally recognised name for the dish
               (e.g. always "Shakshuka" not "Spicy Shakshuka" or "Classic Shakshuka").
               Keep names consistent — the same dish must always return the same name
               regardless of the call.
            5. prepTimeMinutes
               Realistic total preparation + cooking time as an integer (e.g. 20, 45).
               Never null. Never a string like "20 mins". Integer only.
            6. estimatedCalories
               Realistic calorie estimate as an integer.
               Must be within ±10 percent of the target if a calorie target was provided.
               Never null. Integer only.
            7. tags
               Include between 4 and 7 tags total per recipe, split as follows:
               Always include at least:
               — 1 diet type or nutrition tag
               — 1 meal type tag
               — 1 cook style or difficulty tag
               PLUS 2 to 3 free-form tags that meaningfully describe the dish
               and are not already covered by the allowed list above.
               Free-form tags must be lowercase, hyphen-separated
               (e.g. "smoky", "street-food", "tangy", "crispy-edges").
               Never duplicate a concept already covered by an allowed tag.
            8. steps
               Clear cooking instructions. Each step as a separate string.
               Minimum 3 steps, maximum 10 steps.
            9. General
               Return NOTHING except the raw JSON array.
               No markdown fences, no explanation, no extra text.
               Every field must be present on every recipe — never omit a field.
            """;

    public String build(RecipeSuggestionRequestDto request, List<String> normalised) {
        return PROMPT_TEMPLATE
                .replace("{ingredients}", String.join(", ", normalised))
                .replace("{calorieInstruction}", buildCalorieInstruction(request))
                .replace("{preference}", buildPreference(request));
    }

    private String buildCalorieInstruction(RecipeSuggestionRequestDto request) {
        return request.getTargetCalories() != null
                ? "Target calories: " + request.getTargetCalories()
                + " (suggest only recipes within ±10 percent of this value)"
                : "No calorie target — suggest recipes of any calorie range";
    }

    private String buildPreference(RecipeSuggestionRequestDto request) {
        return request.getDietaryPreference() != null
                && request.getDietaryPreference() != DietaryPreference.NONE
                ? request.getDietaryPreference().name().toLowerCase()
                : "none";
    }
}

