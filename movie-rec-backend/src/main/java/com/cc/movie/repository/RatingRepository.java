package com.cc.movie.repository;

import com.cc.movie.entity.Rating;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@EnableMongoRepositories
public interface RatingRepository extends MongoRepository<Rating, String> {
    // 查找该用户是否对该电影评过分
    Optional<Rating> findByUidAndMid(Integer uid, Integer mid);
}
