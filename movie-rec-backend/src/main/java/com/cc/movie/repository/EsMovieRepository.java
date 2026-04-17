package com.cc.movie.repository;

import com.cc.movie.entity.EsMovie;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * ES 专属 Repository。
 * 泛型：<实体类, 主键类型>
 */
@Repository
public interface EsMovieRepository extends ElasticsearchRepository<EsMovie, String> {

}