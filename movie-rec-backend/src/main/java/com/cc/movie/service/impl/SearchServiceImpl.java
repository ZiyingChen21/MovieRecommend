package com.cc.movie.service.impl;

import com.cc.movie.entity.Recommendation;
import com.cc.movie.service.SearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    // 注入 Spring 内置的 JSON 解析器
    @Autowired
    private ObjectMapper objectMapper;

    // 使用原生的 HTTP 客户端，彻底绕过高版本 ES 客户端的限制
    private final RestTemplate restTemplate = new RestTemplate();

    private final String ES_URL = "http://localhost:9200/recommend/_search";

    @Override
    public List<Recommendation> searchMovies(String text, int maxItems) {
        // 处理特殊字符防止 JSON 注入
        String safeText = text.replace("\"", "\\\"");
        // 构造 multi_match 查询
        // 将 fields 扩展为包含 name, director, actors 和 descri
        // 并且通过 ^ 符号给不同字段设置不同的“含金量”
        String dsl = "{\n" +
                "  \"query\": {\n" +
                "    \"multi_match\": {\n" +
                "      \"query\": \"" + safeText + "\",\n" +
                "      \"fields\": [\n" +
                "        \"name^5\", \n" +      // 电影名命中得分翻 5 倍
                "        \"directors^3\", \n" +  // 导演名命中得分翻 3 倍
                "        \"actors^3\", \n" +    // 演员名命中得分翻 3 倍
                "        \"descri\"\n" +        // 简介命中保持原分
                "      ],\n" +
                "      \"type\": \"phrase_prefix\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"size\": " + maxItems + "\n" +
                "}";

        return executeHttpQuery(dsl, "全文检索: " + text);
    }

    @Override
    public List<Recommendation> searchByGenres(String genres, int maxItems) {
        String safeGenres = genres.replace("\"", "\\\"");

        String dsl = "{\n" +
                "  \"query\": {\n" +
                "    \"fuzzy\": {\n" +
                "      \"genres\": \"" + safeGenres + "\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"size\": " + maxItems + "\n" +
                "}";

        return executeHttpQuery(dsl, "标签检索: " + genres);
    }

    /**
     * 核心方法：发送纯净 HTTP 请求并手动解析 JSON
     */
    private List<Recommendation> executeHttpQuery(String dsl, String logTag) {
        try {
            // 1. 组装原生 HTTP 请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(dsl, headers);

            // 2. 发送 POST 请求
            ResponseEntity<String> response = restTemplate.postForEntity(ES_URL, entity, String.class);

            // 3. 解析 ES 返回的原始 JSON 字符串
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode hitsNode = root.path("hits").path("hits");

            log.info("{} -> 命中了 {} 条记录", logTag, root.path("hits").path("total").asInt());

            // 4. 提取 mid 和 _score
            List<Recommendation> recommendations = new ArrayList<>();
            if (hitsNode.isArray()) {
                for (JsonNode hit : hitsNode) {
                    double score = hit.path("_score").asDouble();
                    int mid = hit.path("_source").path("mid").asInt();
                    recommendations.add(new Recommendation(mid, score));
                }
            }
            return recommendations;

        } catch (Exception e) {
            log.error("ES HTTP 请求失败 (请检查 ES 是否启动): {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}