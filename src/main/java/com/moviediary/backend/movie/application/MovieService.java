package com.moviediary.backend.movie.application;

import com.moviediary.backend.movie.dao.MovieRepository;
import com.moviediary.backend.movie.domain.Movie;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {
    private final MovieRepository movieRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        log.info("✅ RestTemplate Bean Injected Successfully");
    }

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    /**
     * 영화 목록 조회 (인기 영화 + 일반 조회)
     */
    public List<Movie> getMovies(Long lastId) {
        // 🔹 Redis에서 인기 영화 10개 가져오기
        List<Long> popularMovieIds = getTopPopularMovies();
        List<Movie> popularMovies = movieRepository.findAllById(popularMovieIds);

        // 🔹 일반 조회 (캐싱된 데이터 확인)
        String cacheKey = "movies:lastId:" + lastId;
        List<Movie> cachedMovies = (List<Movie>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedMovies != null) {
            return cachedMovies;
        }

        // 🔹 DB에서 추가 영화 조회
        List<Movie> movies = movieRepository.findTop10ByIdGreaterThanOrderByIdAsc(lastId);
        if (movies.isEmpty()) {
            fetchAndSaveMoviesFromTMDB();
            movies = movieRepository.findTop10ByIdGreaterThanOrderByIdAsc(lastId);
        }

        // 🔹 조회된 영화 캐싱 (1시간 유지)
        redisTemplate.opsForValue().set(cacheKey, movies, Duration.ofHours(1));

        // 🔹 인기 영화 + 일반 영화 데이터 합쳐서 반환
        return mergeMovieLists(popularMovies, movies);
    }

    /**
     * Redis Sorted Set에 영화 조회수 증가
     */
    public void incrementMovieViewCount(Long movieId) {
        redisTemplate.opsForZSet().incrementScore("movie-popularity", movieId, 1);
    }

    /**
     * Redis에서 인기 영화 목록 가져오기
     */
    public List<Long> getTopPopularMovies() {
        return redisTemplate.opsForZSet().reverseRange("movie-popularity", 0, 9).stream()
                .map(id -> (Long) id)
                .collect(Collectors.toList());
    }

    /**
     * TMDB API에서 최신 영화 목록을 가져와 DB에 저장
     */
    private void fetchAndSaveMoviesFromTMDB() {
        try {
            String url = "https://api.themoviedb.org/3/discover/movie?api_key=" + tmdbApiKey;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");

            if (results != null && !results.isEmpty()) {
                List<Movie> movies = results.stream()
                        .map(this::mapToMovie)
                        .collect(Collectors.toList());
                movieRepository.saveAll(movies);
            }
        } catch (Exception e) {
            log.error("❌ Error fetching movies from TMDB: {}", e.getMessage());
        }
    }

    /**
     * TMDB API에서 인기 영화 목록 가져오기
     */
    private List<Movie> fetchPopularMoviesFromTMDB() {
        try {
            String url = "https://api.themoviedb.org/3/movie/popular?api_key=" + tmdbApiKey;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");

            if (results != null && !results.isEmpty()) {
                return results.stream()
                        .map(this::mapToMovie)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("❌ Error fetching popular movies from TMDB: {}", e.getMessage());
        }
        return List.of();
    }

    /**
     * TMDB API 응답을 Movie 객체로 변환
     */
    private Movie mapToMovie(Map<String, Object> data) {
        try {
            return new Movie(
                    null,
                    String.valueOf(data.get("id")),
                    (String) data.get("title"),
                    data.get("release_date") != null ? LocalDate.parse((String) data.get("release_date")) : null,
                    ((Number) data.getOrDefault("vote_average", 0)).doubleValue(),
                    data.get("genre_ids").toString(),
                    (String) data.get("overview"),
                    "https://image.tmdb.org/t/p/w500" + data.get("poster_path"),
                    "https://image.tmdb.org/t/p/w500" + data.get("backdrop_path"),
                    ((Number) data.getOrDefault("popularity", 0)).doubleValue(),
                    ((Number) data.getOrDefault("vote_count", 0)).intValue(),
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            log.error("❌ Error mapping movie data: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 인기 영화와 일반 영화 리스트를 합치는 메서드
     */
    private List<Movie> mergeMovieLists(List<Movie> popularMovies, List<Movie> normalMovies) {
        return Stream.concat(popularMovies.stream(), normalMovies.stream())
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
    }
}
