package com.cc.movie.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

public class JwtUtils {
    private static final String SECRET_KEY = "CcMovieRecommendSystemSecretKeyMustBeGreaterThan32Characters";

    // 普通登录有效期：2 小时 (单位 ms)
    private static final long NORMAL_EXPIRE_TIME = 2 * 60 * 60 * 1000L;

    // 7 天 单位是 ms ,规定通行证的有效期
    private static final long REMEMBER_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000;

    /**
     * 颁发通行证（生成 Token）
     * 接收： 用户的唯一标识 uid
     * return： 加密后的 Jwt字符串
     */
    public static String generateToken(int uid, boolean isRemember) {
        long expireMillis = isRemember ? REMEMBER_EXPIRE_TIME : NORMAL_EXPIRE_TIME;
        return Jwts.builder()
                .setSubject(String.valueOf(uid))    // uid 存入 Token
                .setIssuedAt(new Date())            // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + expireMillis))  // 过期时间
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)     //使用 HS256 算法和私钥签名
                .compact(); //压缩生成最终的字符串
    }

    /**
     * 解析 Token
     * 接收： token 前端传来的 JWT 字符串
     * return： 解析成功返回 uid， 失败（Or 过期）抛出异常
     * */
    public static Integer getUidFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Integer.parseInt(claims.getSubject());
    }
}
