package com.cc.movie.service;

import com.cc.movie.entity.Movie;

import java.util.List;

public interface UserHistoryService {
    void addHistory(Integer uid, Integer mid);
    List<Movie> getHistoryList(Integer uid);
}
