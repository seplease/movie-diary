package com.moviediary.backend.moviehistory.dao;

import com.moviediary.backend.moviehistory.domain.MovieHistory;
import com.moviediary.backend.movie.dto.MovieProjection;
import com.moviediary.backend.moviehistory.dto.MovieHistoryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieHistoryRepository extends JpaRepository<MovieHistory, Long> {

    // 특정 유저의 감상 기록 조회 (No-Offset)
    @Query("SELECT mh.movie.id AS id, mh.movie.title AS title, mh.movie.posterUrl AS posterUrl, " +
            "mh.movie.popularity AS popularity, mh.review AS review, mh.rating AS rating " +
            "FROM MovieHistory mh WHERE mh.user.id = :userId AND mh.id > :lastId " +
            "ORDER BY mh.id ASC LIMIT 10")
    List<MovieHistoryProjection> findMovieHistoryByUserId(Long userId, Long lastId);

    // 특정 영화 감상 기록 존재 여부
    Optional<MovieHistory> findByUserIdAndMovieId(Long userId, Long movieId);

    // 특정 영화 감상 기록 삭제
    void deleteByUserIdAndMovieId(Long userId, Long movieId);
}
