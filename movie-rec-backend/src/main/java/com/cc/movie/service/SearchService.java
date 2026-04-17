package com.cc.movie.service;

import com.cc.movie.entity.Recommendation;
import java.util.List;

public interface SearchService {

    List<Recommendation> searchMovies(String text, int maxItems);

    List<Recommendation> searchByGenres(String genres, int maxItems);
}