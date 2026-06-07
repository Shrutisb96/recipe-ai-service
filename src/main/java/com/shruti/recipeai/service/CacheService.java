package com.shruti.recipeai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shruti.recipeai.constants.CachePrefix;
import com.shruti.recipeai.constants.CacheTtl;
import com.shruti.recipeai.dto.RecipeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(CachePrefix prefix, String cacheKey,
                     List<RecipeDto> recipes, CacheTtl ttl) {
        try {
            String fullKey = prefix.getValue() + cacheKey;
            String json = objectMapper.writeValueAsString(recipes);
            redisTemplate.opsForValue().set(
                    fullKey, json, ttl.getHours(), TimeUnit.HOURS
            );
            log.info("Cached {} recipes — key: {} TTL: {}h",
                    recipes.size(), fullKey, ttl.getHours());
        } catch (JsonProcessingException e) {
            log.error("Failed to cache recipes for key: {}", cacheKey, e);
        }
    }

    public List<RecipeDto> get(CachePrefix prefix, String cacheKey) {
        try {
            String fullKey = prefix.getValue() + cacheKey;
            Object value = redisTemplate.opsForValue().get(fullKey);
            if (value == null) return null;
            return objectMapper.readValue(
                    value.toString(),
                    new TypeReference<List<RecipeDto>>() {}
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cache for key: {}", cacheKey, e);
            return null;
        }
    }

    public void evict(CachePrefix prefix, String cacheKey) {
        redisTemplate.delete(prefix.getValue() + cacheKey);
    }

    public boolean exists(CachePrefix prefix, String cacheKey) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(prefix.getValue() + cacheKey)
        );
    }
}
