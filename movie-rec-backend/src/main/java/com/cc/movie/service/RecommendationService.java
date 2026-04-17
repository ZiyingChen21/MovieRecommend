package com.cc.movie.service;

import com.cc.movie.entity.Movie;
import com.cc.movie.entity.Recommendation;
import java.util.List;

public interface RecommendationService {
    // 获取实时推荐
    List<Recommendation> getStreamRecommendations(Integer uid, int maxItems);

    // 获取离线协同过滤推荐（作为保底降级方案） ALS
    List<Recommendation> getUserCFRecommendations(Integer uid, int maxItems);

    //电影相似度
    List<Recommendation> getMovieCFRecommendations(Integer mid, int maxItems);

    //基于内容推荐
    List<Recommendation> getContentBasedRecommendations(Integer mid, int maxItems);

    //各类top
    List<Recommendation> getTopGenres(String genres, int maxItem);

    //历史热门
    List<Recommendation> getHistoricalPopular(int maxItems);

    List<Recommendation> getRecentPopular(int maxItems);


    List<Recommendation> findRecommendationByField(String fieldName, Object value, String collectionName, int maxItems);

    List<Movie> buildMovieListWithDualScores(List<Recommendation> recs);

    List<Movie> getHybridFeed(Integer uid, String prefGenre, int limit);

    void fillDualScores(Movie movie, Integer uid);
}