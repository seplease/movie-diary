package com.moviediary.backend.movie.dao;

import com.moviediary.backend.movie.domain.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    // No-Offset 방식으로 영화 10개 조회
    List<Movie> findTop10ByIdGreaterThanOrderByIdAsc(Long lastId);

    // 인기 영화 조회 (DB에서 조회수 기준 정렬)
    @Query("SELECT m FROM Movie m ORDER BY m.popularity DESC LIMIT 10")
    List<Movie> findTop10PopularMovies();
}
