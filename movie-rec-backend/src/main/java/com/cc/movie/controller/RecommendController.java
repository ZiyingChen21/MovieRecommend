package com.cc.movie.controller;

import com.cc.movie.common.R;
import com.cc.movie.entity.Movie;
import com.cc.movie.entity.Recommendation;
import com.cc.movie.service.MovieService;
import com.cc.movie.service.RecommendationService;
import com.cc.movie.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/recommend")
public class RecommendController {
    @Autowired
    private RecommendationService recommendationService;

    @GetMapping("/feed")
    public R<List<Movie>> getFeedRecommendation(@RequestParam(defaultValue = "Action") String prefGenre) {
        Integer uid = UserHolder.getUid();
        log.info("触发多路混合推荐引擎，UID: {}, 偏好标签: {}", uid, prefGenre);

        // 融合
        List<Movie> movies = recommendationService.getHybridFeed(uid, prefGenre, 15);

        return R.success(movies);
    }
    /**
     * 1. 离线协同过滤推荐 (为你推荐)
     * 算法底层：Spark ALS 隐语义模型
     */
    @GetMapping("/als")
    public R<List<Movie>> getUserRecommendation() {
        Integer uid = UserHolder.getUid();
        log.info("获取用户 [{}] 的 ALS 离线推荐", uid);
        List<Recommendation> recs = recommendationService.getUserCFRecommendations(uid, 10);
        return R.success(recommendationService.buildMovieListWithDualScores(recs));
    }

    /**
     * 2. 实时流推荐 (近期热播 / 猜你在追)
     * 算法底层：Kafka + Spark Streaming 实时计算
     */
    @GetMapping("/stream")
    public R<List<Movie>> getStreamRecommendation() {
        Integer uid = UserHolder.getUid();
        log.info("获取用户 [{}] 的实时推荐流", uid);
        List<Recommendation> recs = recommendationService.getStreamRecommendations(uid, 10);
        return R.success(recommendationService.buildMovieListWithDualScores(recs));
    }

    /**
     * 3. 历史热门推荐 (今日 Top 10)
     * 算法底层：MongoDB 离线聚合统计
     */
    @GetMapping("/recent")
    public R<List<Movie>> getRecentHotRecommendation() {
        Integer uid = UserHolder.getUid();
        log.info("获取全网历史热门推荐 Top 10");
        List<Recommendation> recs = recommendationService.getRecentPopular(10);
        return R.success(recommendationService.buildMovieListWithDualScores(recs));
    }

    @GetMapping("/hot")
    public R<List<Movie>> getHotRecommendation() {
        Integer uid = UserHolder.getUid();
        log.info("获取全网历史热门推荐 Top 10");
        List<Recommendation> recs = recommendationService.getHistoricalPopular(10);
        return R.success(recommendationService.buildMovieListWithDualScores(recs));
    }
    /**
     * 4. 类别 Top 推荐 (科幻迷精选等)
     * 根据传入的 genres 标签动态召回
     */
    @GetMapping("/genres")
    public R<List<Movie>> getGenreRecs(@RequestParam(defaultValue = "Action") String genre) {
        log.info("获取分类 [{}] Top 推荐", genre);
        List<Recommendation> recs = recommendationService.getTopGenres(genre, 10);
        return R.success(recommendationService.buildMovieListWithDualScores(recs));
    }

    //相似电影推荐
    @GetMapping("/similar")
    public R<List<Movie>> getSimiarityMovies(@RequestParam("mid") Integer mid) {
        log.info("获取电影 [{}] 的多路混合相似推荐", mid);
        int targetSize = 12;
        List<Recommendation> finalRecs = new ArrayList<>();

        // 用于去重的 Set + 防止自己推自己
        Set<Integer> existMids = new HashSet<>();
        existMids.add(mid);

        List<Recommendation> movieCFRecs = recommendationService.getMovieCFRecommendations(mid, targetSize);
        log.info(movieCFRecs.toString());
        if (movieCFRecs != null) {
            for (Recommendation r : movieCFRecs) {
                finalRecs.add(r);
                existMids.add(r.getMid());
                if (finalRecs.size() >= targetSize) break;
            }
        }
        log.info("第一路 ALS 召回后数量: {}", finalRecs.size());
        // 第二路召回：基于内容推荐 (解决冷启动，补充数量)
        if (finalRecs.size() < targetSize) {
            int needSize = targetSize - finalRecs.size();
            List<Recommendation> contentRecs = recommendationService.getContentBasedRecommendations(mid, needSize);
            if (contentRecs != null) {
                for (Recommendation r : contentRecs) {
                    if (!existMids.contains(r.getMid())) {
                        finalRecs.add(r);
                        existMids.add(r.getMid());
                        needSize--;
                    }
                    if (needSize <= 0) break;
                }
            }
            log.info("第二路 Content-Based 补充后数量: {}", finalRecs.size());
        }
        return R.success(recommendationService.buildMovieListWithDualScores(finalRecs));
    }

}

