package com.cc.movie.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 拦截所有请求
                .allowedOriginPatterns("*") // 允许任意来源（包括前端的 5173 端口）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的请求方式
                .allowedHeaders("*") // 允许的请求头
                .allowCredentials(true) // 允许携带 Cookie/Token
                .maxAge(3600); // 跨域允许时间
    }
}