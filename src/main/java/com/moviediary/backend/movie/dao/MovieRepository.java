package com.moviediary.backend.movie.dao;

import com.moviediary.backend.movie.domain.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findTop10ByIdGreaterThanOrderByIdAsc(Long lastId);
}
