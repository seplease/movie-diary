package com.moviediary.backend.movie.application;

import com.moviediary.backend.movie.dao.MovieRepository;
import com.moviediary.backend.movie.dto.MovieProjection;
import com.moviediary.backend.movie.domain.Movie;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {
    private final MovieRepository movieRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    private static final String TMDB_SEARCH_URL = "https://api.themoviedb.org/3/search/";

    private static final String POPULAR_MOVIE_KEY = "movie-popularity";
    private static final String MOVIE_CACHE_KEY_PREFIX = "movies:lastId:";

    @PostConstruct
    public void init() {
        log.info("✅ RestTemplate Bean Injected Successfully");
        updatePopularMoviesInCache(); // 애플리케이션 시작 시 인기 영화 업데이트
    }

    /**
     * 🎬 영화 목록 조회 (인기 영화 + 일반 조회)
     */
    public List<MovieProjection> getMovies(Long lastId) {
        // 1️⃣ Redis에서 인기 영화 가져오기
        List<Long> popularMovieIds = getTopPopularMovies();
        List<MovieProjection> popularMovies = popularMovieIds.isEmpty() ? new ArrayList<>()
                : movieRepository.findProjectionsByIdIn(popularMovieIds);

        // 2️⃣ 일반 조회 (캐싱된 데이터 확인)
        String cacheKey = MOVIE_CACHE_KEY_PREFIX + lastId;
        List<MovieProjection> cachedMovies = (List<MovieProjection>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedMovies != null) {
            return mergeMovieLists(popularMovies, cachedMovies);
        }

        // 3️⃣ DB에서 추가 영화 조회
        List<MovieProjection> movies = movieRepository.findTop10ProjectionByIdGreaterThanOrderByIdAsc(lastId);
        if (movies.isEmpty()) {
            fetchAndSaveNewMovies();
            movies = movieRepository.findTop10ProjectionByIdGreaterThanOrderByIdAsc(lastId);

            if (movies.isEmpty()) {
                log.warn("⚠️ TMDB API에서 새로운 영화를 가져왔지만 DB에 저장되지 않았습니다.");
            }
        }

        // 4️⃣ 조회된 영화 캐싱 (1시간 유지)
        redisTemplate.opsForValue().set(cacheKey, movies, Duration.ofHours(1));

        // 5️⃣ 인기 영화 + 일반 영화 데이터 합쳐서 반환
        return mergeMovieLists(popularMovies, movies);
    }

    /**
     * 📌 영화 조회 시 인기 점수 증가
     */
    public void incrementMoviePopularity(Long movieId) {
        redisTemplate.opsForZSet().incrementScore(POPULAR_MOVIE_KEY, movieId, 1);
    }

    /**
     * 🎬 Redis에서 인기 영화 목록 가져오기
     */
    public List<Long> getTopPopularMovies() {
        Set<Object> movieIds = redisTemplate.opsForZSet().reverseRange(POPULAR_MOVIE_KEY, 0, 9);
        if (movieIds == null || movieIds.isEmpty()) {
            return updatePopularMoviesInCache(); // 캐시가 없으면 새로 조회
        }
        return movieIds.stream().map(id -> {
            if (id instanceof Integer) {
                return ((Integer) id).longValue(); // Integer → Long 변환
            }
            return (Long) id;
        }).collect(Collectors.toList());
    }

    /**
     * 🔥 매일 새벽 4시에 Redis 인기 영화 목록 갱신
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public List<Long> updatePopularMoviesInCache() {
        log.info("🔥 Refreshing popular movies in Redis...");

        // 기존 Redis 인기 영화 삭제
        redisTemplate.delete(POPULAR_MOVIE_KEY);

        // DB에서 인기 영화 10개 가져오기
        List<MovieProjection> popularMovies = movieRepository.findTop10PopularMovies();

        // Redis에 저장
        for (MovieProjection movie : popularMovies) {
            redisTemplate.opsForZSet().add(POPULAR_MOVIE_KEY, movie.getId().longValue(), movie.getPopularity());
        }

        log.info("✅ Popular movies updated successfully!");
        return popularMovies.stream().map(MovieProjection::getId).collect(Collectors.toList());
    }

    /**
     * 🔥 하루에 한 번 Redis 조회수를 감소시켜서 최근 조회된 영화가 더 높은 순위를 유지하도록 함
     * ⏳ 매일 새벽 3시에 조회수 감소 (오래된 영화의 인기 감소)
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void decayMoviePopularityScores() {
        log.info("🔥 Decreasing movie view counts in Redis...");
        Set<Object> movieIds = redisTemplate.opsForZSet().range(POPULAR_MOVIE_KEY, 0, -1);
        if (movieIds != null) {
            for (Object movieId : movieIds) {
                redisTemplate.opsForZSet().incrementScore(POPULAR_MOVIE_KEY, movieId, -0.1);
            }
        }
        log.info("✅ View counts decreased successfully!");
    }

    /**
     * 🎬 TMDB API에서 최신 영화 목록 가져와 DB 저장 (중복 저장 방지)
     */
    private void fetchAndSaveNewMovies() {
        try {
            log.info("🎬 Fetching movies from TMDB...");
            String url = "https://api.themoviedb.org/3/discover/movie?api_key=" + tmdbApiKey;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");

            if (results == null || results.isEmpty()) {
                log.warn("⚠️ TMDB에서 가져온 영화 데이터가 없음");
                return;
            }

            // 기존 저장된 영화 확인 후 새로운 영화만 저장
            List<String> tmdbIds = results.stream()
                    .map(data -> String.valueOf(data.get("id")))
                    .collect(Collectors.toList());

            Set<String> existingTmdbIds = new HashSet<>(movieRepository.findTmdbIdsByTmdbIdIn(tmdbIds));

            List<Movie> newMovies = results.stream()
                    .filter(data -> !existingTmdbIds.contains(String.valueOf(data.get("id"))))
                    .map(this::mapToMovie)
                    .collect(Collectors.toList());

            if (!newMovies.isEmpty()) {
                movieRepository.saveAll(newMovies);

                List<String> savedTmdbIds = movieRepository.findTmdbIdsByTmdbIdIn(tmdbIds);
                log.info("✅ {}개의 새로운 영화가 DB에 추가됨 (현재 저장된 TMDB ID 개수: {})", newMovies.size(), savedTmdbIds.size());
            } else {
                log.info("✨ 모든 영화가 이미 DB에 존재함 (새로 저장된 영화 없음)");
            }

        } catch (Exception e) {
            log.error("❌ TMDB 영화 업데이트 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 🔹 TMDB API 응답을 Movie 객체로 변환
     */
    private Movie mapToMovie(Map<String, Object> data) {
        try {
            String releaseDateStr = (String) data.get("release_date");
            LocalDate releaseDate = (releaseDateStr != null && !releaseDateStr.isEmpty()) ? LocalDate.parse(releaseDateStr) : null;

            // ✅ 예고편 URL 가져오기
            String trailerUrl = "";
            Map<String, Object> videos = (Map<String, Object>) data.get("videos");
            if (videos != null) {
                var results = (List<Map<String, Object>>) videos.get("results");
                for (Map<String, Object> video : results) {
                    if ("Trailer".equals(video.get("type")) && "YouTube".equals(video.get("site"))) {
                        trailerUrl = "https://www.youtube.com/watch?v=" + video.get("key");
                        break;
                    }
                }
            }

            return new Movie(
                    null,
                    String.valueOf(data.get("id")),
                    (String) data.get("title"),
                    releaseDate,
                    ((Number) data.getOrDefault("vote_average", 0)).doubleValue(),
                    data.get("genre_ids") != null ? data.get("genre_ids").toString() : "[]",
                    (String) data.getOrDefault("overview", ""),
                    "https://image.tmdb.org/t/p/w500" + data.getOrDefault("poster_path", ""),
                    "https://image.tmdb.org/t/p/w500" + data.getOrDefault("backdrop_path", ""),
                    ((Number) data.getOrDefault("popularity", 0)).doubleValue(),
                    ((Number) data.getOrDefault("vote_count", 0)).intValue(),
                    trailerUrl,  // ✅ 예고편 URL 추가
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            log.error("❌ Error mapping movie data: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 🔹 인기 영화와 일반 영화 리스트 합치기
     */
    private List<MovieProjection> mergeMovieLists(List<MovieProjection> popularMovies, List<MovieProjection> normalMovies) {
        return Stream.concat(popularMovies.stream(), normalMovies.stream())
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
    }

    /*
    영화 상세 정보
     */
    public Optional<Movie> getMovieDetails(Long movieId) {
        Optional<Movie> movie = movieRepository.findById(movieId);
        if (movie.isPresent()) {
            return movie;
        }

        // TMDB API에서 상세 정보 가져오기
        try {
            String url = "https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" + tmdbApiKey + "&append_to_response=videos";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> data = response.getBody();

            if (data == null || data.isEmpty()) {
                log.warn("⚠️ TMDB에서 영화 정보를 가져오지 못함 (ID: {})", movieId);
                return Optional.empty();
            }

            // ✅ 예고편 URL 가져오기
            String trailerUrl = "";
            Map<String, Object> videos = (Map<String, Object>) data.get("videos");
            if (videos != null) {
                var results = (List<Map<String, Object>>) videos.get("results");
                for (Map<String, Object> video : results) {
                    if ("Trailer".equals(video.get("type")) && "YouTube".equals(video.get("site"))) {
                        trailerUrl = "https://www.youtube.com/watch?v=" + video.get("key");
                        break;
                    }
                }
            }

            // ✅ `trailerUrl`을 포함하여 `Movie` 객체 생성
            Movie fetchedMovie = new Movie(
                    null,
                    String.valueOf(data.get("id")),
                    (String) data.get("title"),
                    null,
                    ((Number) data.getOrDefault("vote_average", 0)).doubleValue(),
                    data.get("genres") != null ? data.get("genres").toString() : "[]",
                    (String) data.getOrDefault("overview", ""),
                    "https://image.tmdb.org/t/p/w500" + data.getOrDefault("poster_path", ""),
                    "https://image.tmdb.org/t/p/w500" + data.getOrDefault("backdrop_path", ""),
                    ((Number) data.getOrDefault("popularity", 0)).doubleValue(),
                    ((Number) data.getOrDefault("vote_count", 0)).intValue(),
                    trailerUrl,  // ✅ 예고편 URL 추가
                    LocalDateTime.now()
            );

            return Optional.of(fetchedMovie);
        } catch (Exception e) {
            log.error("❌ 영화 상세 정보를 가져오는 중 오류 발생: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 🎬 TMDB API 및 DB 기반 검색 (No-Offset 적용)
     */
    public List<MovieProjection> searchMovies(String query, String type, Long lastId) {
        // 1️⃣ Query가 없으면 로컬 DB에서 영화 검색 (No-Offset 방식)
        if (query == null || query.trim().isEmpty()) {
            return movieRepository.findTop10ProjectionByIdGreaterThanOrderByIdAsc(lastId);
        }

        // 2️⃣ TMDB API 요청 URL 생성 및 호출
        String url = buildTmdbSearchUrl(type, query);
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        Map<String, Object> data = response.getBody();

        if (data == null || !data.containsKey("results")) {
            log.warn("⚠️ TMDB API에서 검색 결과를 가져오지 못함.");
            return List.of();
        }

        // 3️⃣ 결과 매핑 및 반환 (최대 10개)
        return ((List<Map<String, Object>>) data.get("results")).stream()
                .map(this::mapToMovieProjection)
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * ✅ TMDB 검색 URL 생성 (검색 유형에 따른 URL 매핑)
     */
    private String buildTmdbSearchUrl(String type, String query) {
        List<String> validTypes = List.of("movie", "person", "keyword", "collection");
        String searchType = validTypes.contains(type) ? type : "movie";

        return String.format("%s%s?api_key=%s&query=%s&include_adult=false&language=en-US&page=1",
                TMDB_SEARCH_URL, searchType, tmdbApiKey, query);
    }

    /**
     * 🎯 TMDB API 응답을 MovieProjection으로 변환
     */
    private MovieProjection mapToMovieProjection(Map<String, Object> data) {
        return new MovieProjection() {
            @Override
            public Long getId() {
                return Optional.ofNullable(data.get("id")).map(String::valueOf).map(Long::parseLong).orElse(null);
            }

            @Override
            public String getTitle() {
                return Optional.ofNullable((String) data.get("title")).orElse((String) data.getOrDefault("name", "Unknown"));
            }

            @Override
            public String getPosterUrl() {
                return "https://image.tmdb.org/t/p/w500" + Optional.ofNullable((String) data.get("poster_path")).orElse("");
            }

            @Override
            public Double getPopularity() {
                return Optional.ofNullable(data.get("popularity")).map(val -> ((Number) val).doubleValue()).orElse(0.0);
            }
        };
    }
}
