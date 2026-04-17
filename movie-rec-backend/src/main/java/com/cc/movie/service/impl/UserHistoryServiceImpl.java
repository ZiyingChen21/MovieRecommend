package com.cc.movie.service.impl;

import com.cc.movie.entity.Movie;
import com.cc.movie.service.MovieService;
import com.cc.movie.service.UserHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserHistoryServiceImpl implements UserHistoryService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MovieService movieService; // 用于根据 ID 查电影详情

    private static final String HISTORY_KEY_PREFIX = "user:history:";
    @Override
    public void addHistory(Integer uid, Integer mid) {
        String key = HISTORY_KEY_PREFIX + uid;
        long timestamp = System.currentTimeMillis();

        // ZAdd: 将电影ID加入，分数设为当前时间戳
        redisTemplate.opsForZSet().add(key, String.valueOf(mid), timestamp);

        // ZRemRangeByRank: 只保留最新的 20 条记录 (移除排名靠后的旧数据)
        // 排名从 0 开始，保留最后的 20 个元素
        Long size = redisTemplate.opsForZSet().size(key);
        if (size != null && size > 20) {
            redisTemplate.opsForZSet().removeRange(key, 0, size - 21);
        }
    }

    @Override
    public List<Movie> getHistoryList(Integer uid) {
        String key = HISTORY_KEY_PREFIX + uid;

        // ZRevRange: 按分数从高到低取前 20 个 mid
        Set<String> midSet = redisTemplate.opsForZSet().reverseRange(key, 0, 19);
        if (midSet == null || midSet.isEmpty()) return new ArrayList<>();

        List<Integer> mids = midSet.stream().map(Integer::parseInt).collect(Collectors.toList());

        // 从 MongoDB 批量查询电影详情 (由于 Redis 存的是无序 Set，需要按原 mids 顺序重排)
        List<Movie> movies = movieService.getMoviesByMids(mids);

        // 保持 Redis 里的顺序输出
        Map<Integer, Movie> movieMap = movies.stream().collect(Collectors.toMap(Movie::getMid, m -> m));
        return mids.stream().map(movieMap::get).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
