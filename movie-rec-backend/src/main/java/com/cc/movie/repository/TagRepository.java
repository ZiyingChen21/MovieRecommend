package com.cc.movie.repository;

import com.cc.movie.entity.Tags;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Tag 的 MongoDB 数据访问层
 */
@Repository
public interface TagRepository extends MongoRepository<Tags, String> {

    List<Tags> findByMid(Integer mid);

    List<Tags> findByUidAndMid(Integer uid, Integer mid);

    void deleteByUidAndMidAndTag(Integer uid, Integer mid, String tag);
}