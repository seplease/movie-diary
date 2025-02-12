package com.moviediary.backend.moviehistory.application;

import com.moviediary.backend.movie.dao.MovieRepository;
import com.moviediary.backend.moviehistory.dto.MovieHistoryProjection;
import com.moviediary.backend.moviehistory.dao.MovieHistoryRepository;
import com.moviediary.backend.moviehistory.domain.MovieHistory;
import com.moviediary.backend.movie.domain.Movie;
import com.moviediary.backend.user.dao.UserRepository;
import com.moviediary.backend.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MovieHistoryService {
    private final MovieHistoryRepository movieHistoryRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;

    /**
     * 현재 로그인한 사용자 정보 가져오기
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    /**
     * 사용자의 감상 기록 저장 (리뷰 & 평점 포함)
     */
    @Transactional
    public void saveMovieHistory(Long movieId, String review, Integer rating) {
        User user = getCurrentUser();

        Optional<MovieHistory> history = movieHistoryRepository.findByUserIdAndMovieId(user.getId(), movieId);
        if (history.isPresent()) {
            throw new IllegalStateException("이미 감상한 영화입니다.");
        }

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("영화가 존재하지 않습니다."));

        MovieHistory newHistory = MovieHistory.builder()
                .user(user)
                .movie(movie)
                .watchedAt(LocalDateTime.now())
                .lastWatchedAt(LocalDateTime.now())
                .review(review)
                .rating(rating)
                .build();

        movieHistoryRepository.save(newHistory);
    }

    /**
     * 사용자의 감상 기록 조회 (No-Offset)
     */
    public List<MovieHistoryProjection> getMovieHistory(User user, Long lastId) {
        return movieHistoryRepository.findMovieHistoryByUserId(user.getId(), lastId);
    }

    /**
     * 감상 기록 삭제
     */
    @Transactional
    public void deleteMovieHistory(Long movieId) {
        User user = getCurrentUser();
        movieHistoryRepository.deleteByUserIdAndMovieId(user.getId(), movieId);
    }

    /**
     * 다시보기 기능 (lastWatchedAt 업데이트)
     */
    @Transactional
    public void rewatchMovie(Long movieId) {
        User user = getCurrentUser();
        MovieHistory history = movieHistoryRepository.findByUserIdAndMovieId(user.getId(), movieId)
                .orElseThrow(() -> new IllegalArgumentException("감상 기록이 없습니다."));
        history.setLastWatchedAt(LocalDateTime.now());
    }

    /**
     * 감상 기록 수정 (리뷰 및 평점 업데이트)
     */
    @Transactional
    public void updateMovieReview(Long movieId, String review, Integer rating) {
        User user = getCurrentUser();
        MovieHistory history = movieHistoryRepository.findByUserIdAndMovieId(user.getId(), movieId)
                .orElseThrow(() -> new IllegalArgumentException("감상 기록이 없습니다."));
        history.setReview(review);
        history.setRating(rating);
    }
}
