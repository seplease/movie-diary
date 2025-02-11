package com.moviediary.backend.movie.api;

import com.moviediary.backend.movie.application.MovieService;
import com.moviediary.backend.movie.domain.Movie;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Tag(name = "Movie API", description = "영화 조회 관련 API")
public class MovieController {
    private final MovieService movieService;

    @Operation(summary = "영화 목록 조회 (No-Offset)", description = "lastId 이후의 영화 10개를 조회하는 API")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping
    public ResponseEntity<List<Movie>> getMovies(
            @Parameter(description = "마지막으로 조회된 ID (기본값: 0)")
            @RequestParam(required = false, defaultValue = "0") Long lastId) {
        return ResponseEntity.ok(movieService.getMovies(lastId));
    }
}

