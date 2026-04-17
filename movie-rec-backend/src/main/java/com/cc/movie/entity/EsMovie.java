package com.cc.movie.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * ElasticSearch 实体类映射
 * indexName:  "recommende"
 */
@Data
@Document(indexName = "recommend", createIndex = false) // 假设索引已由 DataLoader 创建
public class EsMovie {
    @Id
    private String _id; // ES 内部的文档 ID

    @Field(type = FieldType.Integer)
    private Integer mid; // 电影ID

    // 使用 ik 分词器进行全文检索
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String name;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String descri;

    //导演字段
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String director;

    //演员字段
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String actors;

    @Field(type = FieldType.Keyword)
    private String genres;
}