package com.cc.movie.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Spring Boot 默认会把类名首字母小写（即 movie）去 MongoDB 里找表。
 * Spark 阶段建的表名是大写的 Movie（严格区分大小写）。
 * 如果不加 @Document(collection = "Movie")，后端会查不到任何数据。
 * */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "Movie")
public class Movie {
    @Id // 显式声明为主键
    @JsonIgnore // 发送给前端 JSON 时忽略此乱码般的原生 _id，保证接口整洁
    private String _id;

    private int mid;

    private String name;

    private String descri;

    private String timelong;

    private String issue;

    private String shoot;

    // 全局大众均分
    @Transient
    private Double avgScore;

    // 预测匹配度
    @Transient
    private Double matchRate;

    @Transient
    private Double score; // 作为兼容

    @Field("Language")
    private String language;

    private String genres;

    private String actors;

    private String directors;

    //图片字段
    private String image;
}
