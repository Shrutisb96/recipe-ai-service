package com.shruti.recipeai.entity;

import com.shruti.recipeai.constants.DietaryPreference;
import com.shruti.recipeai.constants.FallbackSource;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "fallback_recipes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FallbackRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "recipe_json", nullable = false, columnDefinition = "TEXT")
    private String recipeJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "dietary_filter", length = 50)
    private DietaryPreference dietaryFilter;

    @Column(name = "ingredient_hash", length = 32, unique = true)
    private String ingredientHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 20)
    private FallbackSource source;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
