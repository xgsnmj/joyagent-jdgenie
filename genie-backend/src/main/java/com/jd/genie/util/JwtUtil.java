package com.jd.genie.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import java.security.Key;
import java.util.Calendar;
import java.util.Date;

/**
 * JWT 工具类
 * 用于生成和验证 JWT Token
 */
@Slf4j
public class JwtUtil {

    // JWT密钥（生产环境应该配置在配置文件中）
    private static final String SECRET_KEY = "genie_jwt_secret_key_20250127_change_me_in_production_environment";
    private static final Key KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    /**
     * 生成Token
     * Token有效期：当天的23:59:59
     *
     * @param userId 用户ID
     * @param username 用户名
     * @return JWT Token字符串
     */
    public static String generateToken(Long userId, String username) {
        Date now = new Date();

        // 计算当天23:59:59作为过期时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Date expiryDate = calendar.getTime();

        log.info("生成Token - 用户: {}, 签发时间: {}, 过期时间: {}",
                 username, now, expiryDate);

        return Jwts.builder()
                .setSubject(username)                    // 设置主题（用户名）
                .claim("userId", userId)                 // 自定义声明（用户ID）
                .setIssuedAt(now)                        // 签发时间
                .setExpiration(expiryDate)               // 过期时间（当天23:59:59）
                .signWith(KEY, SignatureAlgorithm.HS256) // 签名算法和密钥
                .compact();
    }

    /**
     * 从Token中获取用户名
     * @param token JWT Token
     * @return 用户名
     */
    public static String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * 从Token中获取用户ID
     * @param token JWT Token
     * @return 用户ID
     */
    public static Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("userId", Long.class) : null;
    }

    /**
     * 验证Token是否有效
     * @param token JWT Token
     * @return true-有效，false-无效
     */
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析Token获取Claims
     * @param token JWT Token
     * @return Claims对象
     */
    private static Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.error("Token已过期");
            return null;
        } catch (Exception e) {
            log.error("Token解析失败: {}", e.getMessage());
            return null;
        }
    }
}
