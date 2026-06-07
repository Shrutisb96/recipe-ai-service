package com.shruti.recipeai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "suggestion_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "ingredients", nullable = false, columnDefinition = "TEXT")
    private String ingredients;

    @Column(name = "calories")
    private Integer calories;

    @Column(name = "preference", length = 50)
    private String preference;

    @Column(name = "cache_hit")
    private Boolean cacheHit;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
