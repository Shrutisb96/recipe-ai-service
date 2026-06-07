package com.shruti.recipeai.repository;

import com.shruti.recipeai.entity.SuggestionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuggestionLogRepository extends JpaRepository<SuggestionLog, Long> {
}
