package com.cc.movie.service.impl;

import com.cc.movie.entity.Movie;
import com.cc.movie.entity.request.PageResult;
import com.cc.movie.repository.MovieRepository;
import com.cc.movie.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieServiceImpl implements MovieService {
    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Movie getByMid(Integer mid) {
        return movieRepository.findByMid(mid).orElse(null);
    }

    @Override
    public List<Movie> getMoviesByMids(List<Integer> mids) {
        return movieRepository.findByMidIn(mids);
    }

    @Override
    public List<Movie> getNewMovies(int limit) {
        // 使用 MongoTemplate 实现按发行时间排序的“最新电影”查询
        Query query = new Query()
                .with(Sort.by(Sort.Order.desc("issue")))
                .limit(limit);
        return mongoTemplate.find(query, Movie.class);
    }

    @Override
    public PageResult<Movie> getMoviesByFilter(int page, int size, String genre, String year, String sortType) {
        Query query = new Query();

        // 1. 构建类型过滤条件
        // genres : "Action|Sci-Fi" 用正则模糊包含
        if (genre != null && !genre.trim().isEmpty() && !"全部".equals(genre)) {
            query.addCriteria(Criteria.where("genres").regex(genre, "i")); // "i" 忽略大小写
        }

        // 2. 构建年份过滤条件
        if (year != null && !year.trim().isEmpty() && !"全部".equals(year)) {
            if (year.contains("-")) {
                // 处理范围区间，如 "2020-2012"
                String[] parts = year.split("-");
                query.addCriteria(Criteria.where("issue")
                        .gte(parts[0] + "-01-01")
                        .lte(parts[1] + "-12-31"));
            } else if ("更早".equals(year)) {
                query.addCriteria(Criteria.where("issue").lt("2012-01-01"));
            } else {
                // 处理具体单一年份，如 "2026"
                // 转换为: 大于等于 "2026-01-01"，小于等于 "2026-12-31"
                query.addCriteria(Criteria.where("issue")
                        .gte(year + "-01-01")
                        .lte(year + "-12-31"));
            }
        }

        // 3. 先查出符合条件的总条数 (用于前端分页器展示)
        long total = mongoTemplate.count(query, Movie.class);

        // 4. 构建排序条件
        if ("rating".equals(sortType)) {
            // 好评最高：按 MongoDB 原生表里的 score 字段降序
            query.with(Sort.by(Sort.Direction.DESC, "score"));
        } else if ("hot".equals(sortType)) {
            query.with(Sort.by(Sort.Direction.DESC, "mid"));
        } else if ("issue".equals(sortType)) {
            query.with(Sort.by(Sort.Direction.DESC, "issue", "_id"));
        }

        // 5. 构建分页条件 (Spring Data 的页码是从 0 开始的，而前端传的是从 1 开始)
        query.with(PageRequest.of(page - 1, size));

        // 6. 执行最终的组合查询
        List<Movie> movies = mongoTemplate.find(query, Movie.class);

        return new PageResult<>(movies, total);
    }

}
