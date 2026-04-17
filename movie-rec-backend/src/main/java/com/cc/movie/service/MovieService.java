package com.cc.movie.service;

import com.cc.movie.entity.Movie;
import com.cc.movie.entity.request.PageResult;

import java.util.List;

public interface MovieService{
    Movie getByMid(Integer mid);
    List<Movie> getMoviesByMids(List<Integer> mids);
    List<Movie> getNewMovies(int limit);

    PageResult<Movie> getMoviesByFilter(int page, int size, String genre, String year, String sortType);
}
