package com.cc.movie.interceptor;

import com.cc.movie.common.R;
import com.cc.movie.utils.JwtUtils;
import com.cc.movie.utils.UserHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 1. 按照 HTTP 标准，Token 通常放在请求头的 Authorization 字段中
        String token = request.getHeader("Authorization");

        // 2. 如果没传 Token，直接打回
        if (!StringUtils.hasText(token)) {
            returnAuthError(response, "未登录，请登录！");
            return false;
        }

        // 3. 标准的 Token 格式通常是 "Bearer eyJhbG..."，去掉前缀拿到真实 Token
        if (token.startsWith("Bearer")) {
            token = token.substring(7);
        }
        try {
            Integer uid = JwtUtils.getUidFromToken(token);
            UserHolder.setUid(uid);
            log.info("鉴权成功，当前操作用户 UID: {}", uid);
            return true;
        } catch (ExpiredJwtException e) {
            returnAuthError(response, "登录已过期，请重新登录");
            return false;
        } catch (SignatureException e) {
            returnAuthError(response, "无效的通行证");
            return false;
        } catch (Exception e) {
            returnAuthError(response, "鉴权失败，请重新登录");
            return false;
        }
    }

    // 拦截器结束后，清理线程口袋，防止内存泄漏
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.remove();
    }

    // 如果没通过安检，给前端返回标准的 R 格式 JSON
    public void returnAuthError(HttpServletResponse response, String msg) throws Exception{
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);    // 401 状态码
        R<String> error = R.error(msg);

        response.getWriter().write(new ObjectMapper().writeValueAsString(error));
    }
}
