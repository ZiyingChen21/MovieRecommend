package com.cc.movie.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.util.List;

/**
 * 通用 /万能数据传输对象 (Universal DTO) 定义不同表的基类
 * */
@Data
public class MongoRecommendationModel {
    @Id
    private String id;

    private Integer uid;
    private Integer mid;
    private String genres;

    // GenresRecommend 定义的 TopGenres
    @Field("recommendBase")
    private List<Recommendation> recommendBase;

    // UserRecommendation
    @Field("recommendations")
    private List<Recommendation> recommendations;

    // MovieSimilarity / ContentMovieRecommendation
    @Field("similarityMovies")
    private List<Recommendation> movieSimilarity;

    // StreamRecommendation
    @Field("recs")
    private List<Recommendation> recs;

    public List<Recommendation> getEffectiveRecs() {
        if (recommendBase != null) return recommendBase;
        if (recommendations != null) return recommendations;
        if (movieSimilarity != null) return movieSimilarity;
        if (recs != null) return recs;
        return null;
    }
}
