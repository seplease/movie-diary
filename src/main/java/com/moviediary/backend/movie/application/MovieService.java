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

    public List<Movie> getMovies(Long lastId) {
        String cacheKey = "movies:lastId:" + lastId;
        List<Movie> cachedMovies = (List<Movie>) redisTemplate.opsForValue().get(cacheKey);

        if (cachedMovies != null) {
            return cachedMovies;
        }

        List<Movie> movies = movieRepository.findTop10ByIdGreaterThanOrderByIdAsc(lastId);
        if (movies.isEmpty()) {
            fetchAndSaveMoviesFromTMDB();
            movies = movieRepository.findTop10ByIdGreaterThanOrderByIdAsc(lastId);
        }

        redisTemplate.opsForValue().set(cacheKey, movies, Duration.ofHours(1));
        return movies;
    }

    private void fetchAndSaveMoviesFromTMDB() {
        String url = "https://api.themoviedb.org/3/discover/movie?api_key=" + tmdbApiKey;
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");

        List<Movie> movies = results.stream().map(this::mapToMovie).collect(Collectors.toList());
        movieRepository.saveAll(movies);
    }

    private Movie mapToMovie(Map<String, Object> data) {
        return new Movie(
                null,
                String.valueOf(data.get("id")),
                (String) data.get("title"),
                LocalDate.parse((String) data.get("release_date")),
                ((Number) data.get("vote_average")).doubleValue(),
                data.get("genre_ids").toString(),
                (String) data.get("overview"),
                "https://image.tmdb.org/t/p/w500" + data.get("poster_path"),
                "https://image.tmdb.org/t/p/w500" + data.get("backdrop_path"),
                ((Number) data.get("popularity")).doubleValue(),
                ((Number) data.get("vote_count")).intValue(),
                LocalDateTime.now()
        );
    }
}
