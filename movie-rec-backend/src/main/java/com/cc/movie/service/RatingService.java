package com.cc.movie.service;

import com.cc.movie.entity.Rating;

public interface RatingService {
    void saveOrUpdateRating(Integer mid, Double score);

    Rating findMyRating(Integer mid);
}
