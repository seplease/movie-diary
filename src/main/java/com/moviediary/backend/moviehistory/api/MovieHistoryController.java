package com.moviediary.backend.moviehistory.api;

import com.moviediary.backend.moviehistory.dto.MovieHistoryProjection;
import com.moviediary.backend.moviehistory.application.MovieHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies/history")
@RequiredArgsConstructor
@Tag(name = "Movie History API", description = "영화 감상 기록 관련 API")
public class MovieHistoryController {
    private final MovieHistoryService movieHistoryService;

    @Operation(summary = "사용자의 감상 기록 저장 (리뷰 & 평점 포함)")
    @PostMapping("/{movieId}")
    public ResponseEntity<Void> saveMovieHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long movieId,
            @RequestParam(required = false) String review,
            @RequestParam(required = false) Integer rating) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        movieHistoryService.saveMovieHistory(movieId, review, rating);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "사용자의 감상 기록 조회 (No-Offset)")
    @GetMapping
    public ResponseEntity<List<MovieHistoryProjection>> getMovieHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") Long lastId) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(movieHistoryService.getMovieHistory(movieHistoryService.getCurrentUser(), lastId));
    }

    @Operation(summary = "감상 기록 삭제")
    @DeleteMapping("/{movieId}")
    public ResponseEntity<Void> deleteMovieHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long movieId) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        movieHistoryService.deleteMovieHistory(movieId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "다시보기 (lastWatchedAt 업데이트)")
    @PatchMapping("/{movieId}/rewatch")
    public ResponseEntity<Void> rewatchMovie(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long movieId) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        movieHistoryService.rewatchMovie(movieId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "리뷰 및 평점 수정")
    @PatchMapping("/{movieId}/review")
    public ResponseEntity<Void> updateMovieReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long movieId,
            @RequestParam String review,
            @RequestParam Integer rating) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        movieHistoryService.updateMovieReview(movieId, review, rating);
        return ResponseEntity.ok().build();
    }
}
