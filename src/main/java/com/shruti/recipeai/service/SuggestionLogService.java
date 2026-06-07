package com.shruti.recipeai.service;

import com.shruti.recipeai.dto.request.RecipeSuggestionRequestDto;
import com.shruti.recipeai.entity.SuggestionLog;
import com.shruti.recipeai.repository.SuggestionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuggestionLogService {

    private final SuggestionLogRepository suggestionLogRepository;

    @Async
    public void log(Long userId, List<String> ingredients,
                    RecipeSuggestionRequestDto request, boolean cacheHit) {
        try {
            SuggestionLog entry = SuggestionLog.builder()
                    .userId(userId)
                    .ingredients(String.join(",", ingredients))
                    .calories(request.getTargetCalories())
                    .preference(request.getDietaryPreference().name())
                    .cacheHit(cacheHit)
                    .createdAt(LocalDateTime.now())
                    .build();
            suggestionLogRepository.save(entry);

        } catch (Exception e) {
            // TODO v2 — publish to Kafka dead-letter topic instead of just logging (TBD)
            log.error("[SUGGESTION_LOG_DB_FAILED] userId={} ingredients={} calories={} preference={} cacheHit={} error={}",
                    userId,
                    String.join(",", ingredients),
                    request.getTargetCalories(),
                    request.getDietaryPreference(),
                    cacheHit,
                    e.getMessage()
            );
        }
    }
}
