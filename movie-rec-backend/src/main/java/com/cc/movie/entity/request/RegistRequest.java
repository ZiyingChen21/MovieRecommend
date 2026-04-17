package com.cc.movie.entity.request;

import lombok.Data;

@Data
public class RegistRequest extends BaseUserRequest{
    private String confirmPassword; // 注册特有字段
}
