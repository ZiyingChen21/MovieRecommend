package com.cc.movie.repository;


import com.cc.movie.entity.Movie;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@EnableMongoRepositories
public interface MovieRepository extends MongoRepository<Movie, String> {
    Optional<Movie> findByMid(Integer mid);

    // 批量查询电影（用于把推荐列表的 ID 集合转为电影对象集合）
    List<Movie> findByMidIn(List<Integer> mids);
}
