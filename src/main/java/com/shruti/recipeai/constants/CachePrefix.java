package com.shruti.recipeai.constants;

import lombok.Getter;

@Getter
public enum CachePrefix {
    RECIPES("recipeai:suggest:"),
    FAVOURITES("recipeai:favourites:"),
    RECURRING("recipeai:recurring:"),
    CALORIES("recipeai:calories:");

    private final String value;

    CachePrefix(String value) { this.value = value; }

}
