package com.cc.movie.service.impl;

import com.cc.movie.entity.AverageRating;
import com.cc.movie.entity.MongoRecommendationModel;
import com.cc.movie.entity.Movie;
import com.cc.movie.entity.Recommendation;
import com.cc.movie.service.MovieService;
import com.cc.movie.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MovieService movieService;

    //实时推荐  根据 field 字段
    @Override
    public List<Recommendation> getStreamRecommendations(Integer uid, int maxItems) {
        return findRecommendationByField("uid", uid, "StreamRecommendation", maxItems);
    }

    // ALS - 用户预测评分
    @Override
    public List<Recommendation> getUserCFRecommendations(Integer uid, int maxItems) {
        return findRecommendationByField("uid", uid, "UserRecommendation", maxItems);
    }

    // ALS - 电影相似度
    @Override
    public List<Recommendation> getMovieCFRecommendations(Integer mid, int maxItems) {
        return findRecommendationByField("mid", mid, "MovieSimilarity", maxItems);
    }

    // 基于内容的推荐 TF-IDF
    @Override
    public List<Recommendation> getContentBasedRecommendations(Integer mid, int maxItems) {
        return findRecommendationByField("mid", mid, "ContentMovieRecommendation", maxItems);
    }

    // 统计推荐 genre TopN
    @Override
    public List<Recommendation> getTopGenres(String genres, int maxItem) {
        return findRecommendationByField("genres", genres, "TopGenres", maxItem);
    }

    @Override
    public List<Recommendation> getHistoricalPopular(int maxItems) {
        Query query = new Query().with(Sort.by(Sort.Direction.DESC, "count")).limit(maxItems);
        return mongoTemplate.find(query, MongoRecommendationModel.class, "HistoricalPopularity").stream()
                .map(model -> new Recommendation(model.getMid(), 0.0))
                .collect(Collectors.toList());
    }

    @Override
    public List<Recommendation> getRecentPopular(int maxItems) {
        // 1. 扩大查询范围，防止去重后数量不够
        Query query = new Query().with(Sort.by(Sort.Direction.DESC, "count")).limit(maxItems * 3);
        List<MongoRecommendationModel> models = mongoTemplate.find(query, MongoRecommendationModel.class, "RecentPopularity");

        // 2. 使用 Set 进行 mid 去重过滤
        Set<Integer> existMids = new HashSet<>();
        List<Recommendation> result = new ArrayList<>();

        for (MongoRecommendationModel model : models) {
            int mid = model.getMid();
            if (!existMids.contains(mid)) {
                result.add(new Recommendation(mid, 0.0));
                existMids.add(mid);

                if (result.size() >= maxItems) {
                    break;
                }
            }
        }

        return result;
    }

    @Override
    public List<Recommendation> findRecommendationByField(String fieldName, Object value, String collectionName, int maxItems) {
        Query query = new Query(Criteria.where(fieldName).is(value));
        MongoRecommendationModel model = mongoTemplate.findOne(query, MongoRecommendationModel.class, collectionName);

        if (model == null || model.getEffectiveRecs() == null) {
            return new ArrayList<>();
        }
        List<Recommendation> recommendations = model.getEffectiveRecs();
        return recommendations.size() > maxItems ? recommendations.subList(0, maxItems) : recommendations;
    }

    /**
     * 高性能批量组装：大众均分 + 预测匹配度
     */
    @Override
    public List<Movie> buildMovieListWithDualScores(List<Recommendation> recs) {
        if (recs == null || recs.isEmpty()) return new ArrayList<>();

        List<Integer> mids = recs.stream().map(Recommendation::getMid).collect(Collectors.toList());
        List<Movie> movies = movieService.getMoviesByMids(mids);
        Map<Integer, Movie> movieMap = movies.stream().collect(Collectors.toMap(Movie::getMid, m -> m));

        // 批量查均分
        Query query = new Query(Criteria.where("mid").in(mids));
        List<AverageRating> avgRatings = mongoTemplate.find(query, AverageRating.class);
        Map<Integer, Double> avgMap = avgRatings.stream().collect(Collectors.toMap(AverageRating::getMid, AverageRating::getAverage));

        List<Movie> result = new ArrayList<>();
        for (Recommendation rec : recs) {
            Movie movie = movieMap.get(rec.getMid());
            if (movie == null) continue;

            //  调用统一的注入逻辑
            injectDualScores(movie, rec.getScore(), avgMap.get(movie.getMid()));
            result.add(movie);
        }
        return result;
    }

    private void injectDualScores(Movie movie, Double predictScore, Double avg) {
        if (avg != null) {
            movie.setAvgScore(Math.round(avg * 10.0) / 10.0);
        }

        if (predictScore != null) {
            // 留一位小数
            double rawMatch = (predictScore / 5.0) * 100;
            double formattedMatch = Math.round(rawMatch * 10.0) / 10.0;

            // 边界保护
            formattedMatch = Math.min(formattedMatch, 99.9);
            formattedMatch = Math.max(formattedMatch, 60.0);

            movie.setMatchRate(formattedMatch);
        }
    }

    @Override
    public List<Movie> getHybridFeed(Integer uid, String prefGenre, int limit) {
        List<Recommendation> streamRecs = getStreamRecommendations(uid, 50);
        List<Recommendation> alsRecs = getUserCFRecommendations(uid, 50);

        List<Recommendation> finalRecs = new ArrayList<>();

        // 1 冷启动检测
        if (streamRecs.isEmpty() && alsRecs.isEmpty()) {
            // 纯新用户
            List<Recommendation> coldRecs = getTopGenres(prefGenre, limit);

            if (coldRecs == null || coldRecs.isEmpty()) {
                coldRecs = getRecentPopular(limit);
            }
            return buildMovieListWithDualScores(coldRecs);
        }

        // 加权融合排序
        double wStream = 0.7;
        double wAls = 0.3;

        // 排序打分、保存要展示的原始最高分
        Map<Integer, Double> sortScoreMap = new HashMap<>();
        Map<Integer, Double> displayScoreMap = new HashMap<>();

        // 注入 ALS 离线分数
        for (Recommendation rec : alsRecs) {
            sortScoreMap.put(rec.getMid(), rec.getScore() * wAls);
            displayScoreMap.put(rec.getMid(), rec.getScore()); // 记录原始分
        }

        // 注入 Stream 实时分数
        for (Recommendation rec : streamRecs) {
            double currentSortScore = sortScoreMap.getOrDefault(rec.getMid(), 0.0);
            sortScoreMap.put(rec.getMid(), currentSortScore + (rec.getScore() * wStream));

            // 记录要展示的原始分
            double currentDisplayScore = displayScoreMap.getOrDefault(rec.getMid(), 0.0);
            displayScoreMap.put(rec.getMid(), Math.max(currentDisplayScore, rec.getScore()));
        }

        // 加权分排序，截取 Top N  但存入对象的是“展示分”
        finalRecs = sortScoreMap.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    int mid = entry.getKey();
                    double rawDisplayScore = displayScoreMap.get(mid);
                    return new Recommendation(mid, rawDisplayScore);
                })
                .collect(Collectors.toList());

        return buildMovieListWithDualScores(finalRecs);
    }

    @Override
    public void fillDualScores(Movie movie, Integer uid) {
        if (movie == null) return;
        int mid = movie.getMid();

        Query avgQuery = new Query(Criteria.where("mid").is(mid));
        AverageRating avgRating = mongoTemplate.findOne(avgQuery, AverageRating.class, "AverageRating");
        Double avg = avgRating != null ? avgRating.getAverage() : null;

        Double predictScore = null;
        List<Recommendation> streamRecs = getStreamRecommendations(uid, 100);
        predictScore = streamRecs.stream().filter(r -> r.getMid() == mid).map(Recommendation::getScore).findFirst().orElse(null);

        if (predictScore == null) {
            List<Recommendation> alsRecs = getUserCFRecommendations(uid, 100);
            predictScore = alsRecs.stream().filter(r -> r.getMid() == mid).map(Recommendation::getScore).findFirst().orElse(null);
        }

        injectDualScores(movie, predictScore, avg);
    }
}
