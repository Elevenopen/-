package com.flowcontrol.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 * 提供 Token 生成、解析、验证功能
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-ttl}")
    private Long accessTokenTtl;

    @Value("${jwt.refresh-token-ttl}")
    private Long refreshTokenTtl;

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        // 密钥长度至少256位
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 Access Token（用户身份令牌，15分钟有效期）
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param role     角色
     * @return JWT Token字符串
     */
    public String generateAccessToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>(4);
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("tokenType", "access");

        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenTtl);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 生成 Refresh Token（刷新令牌，7天有效期）
     */
    public String generateRefreshToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>(3);
        claims.put("userId", userId);
        claims.put("tokenType", "refresh");

        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenTtl);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 Token，提取全部 Claims
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.warn("Token解析失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 从 Token 中提取用户名
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 从 Token 中提取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Object userId = parseToken(token).get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return (Long) userId;
    }

    /**
     * 从 Token 中提取角色
     */
    public String getRoleFromToken(String token) {
        return (String) parseToken(token).get("role");
    }

    /**
     * 判断 Token 是否已过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = parseToken(token).getExpiration();
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token, String expectedUsername) {
        try {
            String username = getUsernameFromToken(token);
            return username.equals(expectedUsername) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
