package com.moviediary.backend.movie.dto;

public interface MovieProjection {
    Long getId();
    String getTitle();
    String getPosterUrl();
    Double getPopularity();
}
