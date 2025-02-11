package com.moviediary.backend.recommendation.domain;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("recommendations")
public class RecommendationCache {
    @Id
    private Long userId;

    private List<Long> recommendedMovieIds;

    private LocalDateTime cachedAt = LocalDateTime.now();
}
