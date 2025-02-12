package com.moviediary.backend.moviehistory.dto;

public interface MovieHistoryProjection {
    Long getId();
    String getTitle();
    String getPosterUrl();
    Double getPopularity();
    String getReview();
    Integer getRating();
}
