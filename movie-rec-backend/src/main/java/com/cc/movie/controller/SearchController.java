package com.cc.movie.controller;

import com.cc.movie.common.R;
import com.cc.movie.entity.Movie;
import com.cc.movie.entity.Recommendation;
import com.cc.movie.service.MovieService;
import com.cc.movie.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @Autowired
    private MovieService movieService;

    /**
     * 前端搜索调用的接口
     */
    @GetMapping("/text")
    public R<List<Movie>> searchByText(@RequestParam String query,
                                       @RequestParam(defaultValue = "20") int size) {
        log.info("接收到搜索请求, 关键字: {}, 召回数量: {}", query, size);

        // ES 查匹配的电影 ID 和 得分
        List<Recommendation> esRecs = searchService.searchMovies(query, size);

        if (esRecs == null || esRecs.isEmpty()) {
            return R.success(new ArrayList<>());
        }

        // MongoDB 查电影的完整信息
        List<Movie> movies = movieService.getMoviesByMids(esRecs.stream().map(Recommendation::getMid).collect(Collectors.toList()));

        return R.success(movies);
    }
}