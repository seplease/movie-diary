package com.moviediary.backend.movie.dao;

import com.moviediary.backend.movie.domain.Movie;
import com.moviediary.backend.movie.dto.MovieProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    // No-Offset 방식으로 영화 10개 조회 (Projection 사용)
    @Query("SELECT m.id AS id, m.title AS title, m.posterUrl AS posterUrl, m.popularity AS popularity FROM Movie m WHERE m.id > :lastId ORDER BY m.id ASC LIMIT 10")
    List<MovieProjection> findTop10ProjectionByIdGreaterThanOrderByIdAsc(Long lastId);

    // 인기 영화 조회 (Projection 사용)
    @Query("SELECT m.id AS id, m.title AS title, m.posterUrl, m.popularity AS popularity FROM Movie m ORDER BY m.popularity DESC LIMIT 10")
    List<MovieProjection> findTop10PopularMovies();

    // ✅ 특정 ID 리스트에 해당하는 MovieProjection 조회
    @Query("SELECT m.id AS id, m.title AS title, m.posterUrl, m.popularity AS popularity FROM Movie m WHERE m.id IN :ids")
    List<MovieProjection> findProjectionsByIdIn(List<Long> ids);

    // ✅ 특정 ID 리스트에 해당하는 Movie 엔티티 조회
    List<Movie> findAllByIdIn(List<Long> ids);

    // ✅ 이미 저장된 TMDB ID 조회 (중복 저장 방지)
    @Query("SELECT m.tmdbId FROM Movie m WHERE m.tmdbId IN :tmdbIds")
    List<String> findTmdbIdsByTmdbIdIn(List<String> tmdbIds);
}
