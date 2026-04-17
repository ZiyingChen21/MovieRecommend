package com.cc.movie.entity.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class LoginRequest extends BaseUserRequest{
    private Boolean rememberMe; // 登录特有字段 默认 false
}
