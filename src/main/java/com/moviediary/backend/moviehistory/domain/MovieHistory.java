package com.moviediary.backend.moviehistory.domain;

import com.moviediary.backend.movie.domain.Movie;
import com.moviediary.backend.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "movie_history", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "movie_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(nullable = false)
    private LocalDateTime watchedAt;

    private LocalDateTime lastWatchedAt;

    // 🎯 기존 리뷰 기능을 포함 (Review 엔티티 대체)
    @Column(columnDefinition = "TEXT")
    private String review;

    private Integer rating;
}
