package com.shruti.recipeai.common;

import com.shruti.recipeai.constants.DietaryPreference;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class CacheKeyGenerator {

    // Full cache key — includes ingrdients, calories and preference
    public String generateRecipeSuggestionKey(List<String> ingredients,
                                              Integer calories,
                                              DietaryPreference preference) {
        String raw = String.join(",", ingredients) + "|"
                + (calories != null ? calories : "any") + "|"
                + (preference != null ? preference : DietaryPreference.NONE);
        return DigestUtils.md5Hex(raw);
    }

    // Ingredient hash only — used for fallback matching and deduplication
    public String generateIngredientHash(List<String> ingredients) {
        return DigestUtils.md5Hex(String.join(",", ingredients));
    }
}
