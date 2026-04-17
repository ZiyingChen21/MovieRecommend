package com.cc.movie.service.impl;

import com.cc.movie.entity.Rating;
import com.cc.movie.repository.RatingRepository;
import com.cc.movie.service.RatingService;
import com.cc.movie.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RatingServiceImpl implements RatingService {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final int REDIS_MOVIE_RATING_SIZE = 20;
    @Autowired
    private RatingRepository ratingRepository;
    @Override
    public void saveOrUpdateRating(Integer mid, Double score) {
        Integer uid = UserHolder.getUid();
        int timestamp = (int) (System.currentTimeMillis() / 1000);

        //存入 MongoDB
        saveOrUpdateToMongo(uid, mid, score, timestamp);

        //更新 redis 最近 K 次评分队列
        updateRedisQueue(uid, mid, score);

        //触发 Kafka 实时推荐流
        sendToKafka(uid, mid, score, timestamp);
    }

    private void saveOrUpdateToMongo(Integer uid, Integer mid, Double score, int timestamp) {        //构造条件查询 存在该评分的数据
        // 根据 uid 和 mid 唯一定位一条评分记录
        Query query = new Query(Criteria.where("uid").is(uid).and("mid").is(mid));

        // 只更新分数和时间戳
        Update update = new Update()
                .set("score", score)
                .set("timestamp", timestamp);

        // 找到就更新，找不到就以 query 里的条件为基础插入新记录
        mongoTemplate.upsert(query, update, Rating.class);
        log.debug("MongoDB 评分记录 (uid:{}, mid:{}) 处理完成", uid, mid);
    }

    /**
     * 更新 Redis 队列 (使用 Spring Boot 的 opsForList)
     */
    private void updateRedisQueue(Integer uid, Integer mid, Double score) {
        String redisKey = "uid:" + uid;
        String redisValue = mid + ":" + score;

        // 将最新打分从左侧推入队列
        redisTemplate.opsForList().leftPush(redisKey, redisValue);

        // 保持队列长度：使用 trim 裁剪，永远只保留最新的 40 条数据
        redisTemplate.opsForList().trim(redisKey, 0, REDIS_MOVIE_RATING_SIZE - 1);
        log.debug("Redis 特征更新完成");
    }

    private void sendToKafka(Integer uid, Integer mid, Double score, long timestamp) {
        String message = uid + "|" + mid + "|" + score + "|" + timestamp;
        kafkaTemplate.send("recommend", message);
        log.info("Kafka 消息已发送至 recommender 主题: {}", message);
    }

    // 在 RatingServiceImpl 中实现该方法
    @Override
    public Rating findMyRating(Integer mid) {
        // 1. 获取当前登录用户的 ID
        Integer uid = UserHolder.getUid();

        // 2. 从数据库中查询该用户对该电影的评分记录
        return ratingRepository.findByUidAndMid(uid, mid).orElse(null);
    }
}
