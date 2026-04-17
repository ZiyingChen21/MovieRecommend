package com.cc.movie.entity.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class MovieRatingRequest {
    // 无uid，从 UserHolder 中通过 token 获取
    @NotNull(message = "电影 ID 不能为空！")
    private Integer mid;

    @NotNull(message = "评分不能为空！")
    private Double score;
}
