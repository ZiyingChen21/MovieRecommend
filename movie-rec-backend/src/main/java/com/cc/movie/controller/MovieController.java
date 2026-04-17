package com.cc.movie.controller;

import com.cc.movie.common.R;
import com.cc.movie.entity.Movie;
import com.cc.movie.entity.request.PageResult;
import com.cc.movie.service.MovieService;
import com.cc.movie.service.RecommendationService;
import com.cc.movie.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/movie")
public class MovieController {
    @Autowired
    private MovieService movieService;

    @Autowired
    private RecommendationService recommendationService;

    @GetMapping("/info/{mid}")
    public R<Movie> getMovieInfo(@PathVariable Integer mid) {
        Movie movie = movieService.getByMid(mid);
        if (movie == null) return R.error("电影消失在黑洞中了");

        Integer uid = UserHolder.getUid();
        recommendationService.fillDualScores(movie, uid);

        return R.success(movie);
    }

    @GetMapping("/new")
    public R<List<Movie>> getNewMovies(@RequestParam(defaultValue = "10") int n) {
        return R.success(movieService.getNewMovies(n));
    }

    /**
     * 高分电影片库：多条件组合分页查询
     * 前端请求示例: GET /movie/filter?page=1&size=24&genre=Action&sort=rating
     */
    @GetMapping("/filter")
    public R<PageResult<Movie>> filterMovies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String year,
            @RequestParam(defaultValue = "rating") String sort) {

        PageResult<Movie> result = movieService.getMoviesByFilter(page, size, genre, year, sort);
        return R.success(result);

    }
}
