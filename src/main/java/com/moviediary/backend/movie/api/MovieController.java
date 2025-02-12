package com.moviediary.backend.movie.api;

import com.moviediary.backend.movie.application.MovieService;
import com.moviediary.backend.movie.domain.Movie;
import com.moviediary.backend.movie.dto.MovieProjection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Tag(name = "Movie API", description = "영화 관련 API")
public class MovieController {
    private final MovieService movieService;

    @Operation(
            summary = "영화 목록 조회 (No-Offset)",
            description = "lastId 이후의 영화 10개를 조회하는 API. \n\n"
                    + "- 기본적으로 `lastId=0`이면 처음 10개의 영화를 반환합니다.\n"
                    + "- 이후 `lastId`를 이용해 다음 10개의 데이터를 조회할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "영화 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (lastId가 유효하지 않음)"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping
    public ResponseEntity<List<MovieProjection>> getMovies(
            @Parameter(description = "마지막으로 조회된 영화 ID (기본값: 0)", example = "15")
            @RequestParam(required = false, defaultValue = "0") Long lastId) {
        return ResponseEntity.ok(movieService.getMovies(lastId));
    }

    @Operation(
            summary = "영화 상세 정보 조회",
            description = "특정 영화의 상세 정보를 TMDB API에서 가져와 반환합니다.\n\n"
                    + "- 데이터베이스에 영화가 없을 경우 TMDB에서 데이터를 가져옵니다.\n"
                    + "- 영화 예고편 링크가 포함됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "영화 상세 정보 조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 ID의 영화 정보가 존재하지 않음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{movieId}")
    public ResponseEntity<Movie> getMovieDetails(
            @Parameter(description = "조회할 영화의 ID", example = "550") @PathVariable Long movieId) {
        Optional<Movie> movieDetails = movieService.getMovieDetails(movieId);
        return movieDetails.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
