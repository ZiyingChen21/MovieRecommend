package com.cc.movie.service.impl;

import com.cc.movie.entity.Movie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TmdbSyncService {

    @Autowired
    private MongoTemplate mongoTemplate;

    // TMDB API Key
    private static final String TMDB_API_KEY = "aa3ca2f0361eee4f67d0664cc909659f";

    // 配置本地代理端口
    private static final int PROXY_PORT = 7890;

    public void syncAllMoviePosters() {
        log.info("开始执行 TMDB 海报同步任务 (代理模式)...");

        List<Movie> movies = mongoTemplate.findAll(Movie.class);

        // 为 RestTemplate 配置代理
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", PROXY_PORT));
        factory.setProxy(proxy);
        factory.setConnectTimeout(10000); // 10秒连接超时
        factory.setReadTimeout(10000);    // 10秒读取超时

        RestTemplate restTemplate = new RestTemplate(factory);


        int successCount = 0;

        for (Movie movie : movies) {
            // 如果已经有海报了，直接跳过 (支持断点续传)
            if (movie.getImage() != null && !movie.getImage().isEmpty()) {
                continue;
            }

            // 清洗电影名中的年份
            String cleanName = movie.getName().replaceAll("\\(\\d{4}\\)", "").trim();
            String url = "https://api.themoviedb.org/3/search/movie?api_key=" + TMDB_API_KEY + "&query=" + cleanName + "&language=zh-CN";

            try {
                Map<String, Object> response = restTemplate.getForObject(url, Map.class);

                if (response != null && response.containsKey("results")) {
                    List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

                    if (!results.isEmpty()) {
                        String posterPath = (String) results.get(0).get("poster_path");
                        if (posterPath != null) {
                            String fullImageUrl = "https://image.tmdb.org/t/p/w500" + posterPath;
                            movie.setImage(fullImageUrl);
                            mongoTemplate.save(movie);

                            log.info("✅ 成功更新: [{}] -> {}", cleanName, fullImageUrl);
                            successCount++;
                        } else {
                            log.warn("⚠️ 电影 [{}] 查到了，但没有海报封面", cleanName);
                        }
                    } else {
                        log.warn("❌ TMDB 未搜到此电影: [{}]", cleanName);
                    }
                }

                // 核心防封：每次请求后休眠 250 毫秒
                Thread.sleep(250);

            } catch (Exception e) {
                log.error("请求异常，电影: [{}]，错误原因: {}", cleanName, e.getMessage());
            }
        }

        log.info("补全任务结束！本次共成功补充了 {} 部电影的海报！", successCount);
    }
}