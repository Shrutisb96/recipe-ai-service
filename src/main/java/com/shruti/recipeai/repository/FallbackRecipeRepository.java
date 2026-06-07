package com.shruti.recipeai.repository;

import com.shruti.recipeai.entity.FallbackRecipe;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FallbackRecipeRepository
        extends JpaRepository<FallbackRecipe, Long> {

    // Step 1 — exact ingredient match
    @Query(value = """
        SELECT * FROM fallback_recipes
        WHERE ingredient_hash = :hash
        ORDER BY RANDOM()
        LIMIT :limit
        """, nativeQuery = true)
    List<FallbackRecipe> findByIngredientHash(
            @Param("hash") String hash,
            @Param("limit") int limit
    );

    // Step 2 — dietary filter match
    @Query(value = """
        SELECT * FROM fallback_recipes
        WHERE dietary_filter = :filter
        ORDER BY RANDOM()
        LIMIT :limit
        """, nativeQuery = true)
    List<FallbackRecipe> findByDietaryFilter(
            @Param("filter") String filter,
            @Param("limit") int limit
    );

    // Step 3 — absolute last resort
    @Query(value = """
        SELECT * FROM fallback_recipes
        ORDER BY RANDOM()
        LIMIT :limit
        """, nativeQuery = true)
    List<FallbackRecipe> findRandom(@Param("limit") int limit);

    // Nightly job
    boolean existsByIngredientHash(String ingredientHash);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO fallback_recipes
            (name, recipe_json, dietary_filter, ingredient_hash, source, created_at)
        VALUES
            (:name, :recipeJson, :dietaryFilter, :ingredientHash, :source, NOW())
        ON CONFLICT (ingredient_hash)
        DO UPDATE SET
            recipe_json = EXCLUDED.recipe_json,
            name = EXCLUDED.name
        """, nativeQuery = true)
    void upsert(
            @Param("name") String name,
            @Param("recipeJson") String recipeJson,
            @Param("dietaryFilter") String dietaryFilter,
            @Param("ingredientHash") String ingredientHash,
            @Param("source") String source
    );
}
