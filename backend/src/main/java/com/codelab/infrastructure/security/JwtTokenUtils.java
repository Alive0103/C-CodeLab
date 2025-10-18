package com.codelab.infrastructure.security;


import com.codelab.infrastructure.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenUtils {


    private final JwtConfig jwtConfig;
    private final RedisTemplate redisTemplate;

    /**
     * 生成JWT token
     * @param username
     * @return
     */
    public String generateToken(String username) {
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + jwtConfig.getExpiration();

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(nowMillis))
                .setExpiration(new Date(expMillis))
                .signWith(jwtConfig.getSigningKey())
                .compact();
    }


    /**
     * 从token中提取用户名
     * @param token
     * @return
     */
    public String getUsernameFromToken(String token) {
        try {
            String cleanToken = cleanToken(token);
            if (cleanToken == null || cleanToken.isEmpty()) {
                throw new IllegalArgumentException("Token is empty or null");
            }

            String username = Jwts.parserBuilder()
                    .setSigningKey(jwtConfig.getSigningKey())
                    .build()
                    .parseClaimsJws(cleanToken)
                    .getBody()
                    .getSubject();
            log.debug("Parsed username from token: {}", username);
            return username;
        } catch (Exception e) {
            log.error("Failed to parse username from token: {}", token, e);
            throw e;
        }
    }

    /**
     * 从token中提取用户名（忽略过期时间，用于刷新场景）
     * @param token
     * @return
     */
    public String getUsernameFromTokenIgnoringExpiration(String token) {
        try {
            String cleanToken = cleanToken(token);
            if (cleanToken == null || cleanToken.isEmpty()) {
                throw new IllegalArgumentException("Token is empty or null");
            }

            // 解析token但不验证过期时间
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtConfig.getSigningKey())
                    .build()
                    .parseClaimsJws(cleanToken)
                    .getBody();
            
            String username = claims.getSubject();
            log.debug("Parsed username from token (ignoring expiration): {}", username);
            return username;
        } catch (Exception e) {
            log.error("Failed to parse username from token (ignoring expiration): {}", token, e);
            throw e;
        }
    }

    /**
     *  验证token的有效性
     * @param token
     * @return
     */
    public boolean validateToken(String token) {
        try {
            String cleanToken = cleanToken(token);

            // 获取token中的用户名
            String username = getUsernameFromToken(cleanToken);

            // 检查该用户当前有效的token中是否包含此token
            String userTokensKey = "user_tokens:" + username;
            Boolean isMember = redisTemplate.opsForSet().isMember(userTokensKey, cleanToken);

            if (Boolean.TRUE.equals(isMember)) {
                // 验证token签名和过期时间
                Jwts.parserBuilder()
                        .setSigningKey(jwtConfig.getSigningKey())
                        .build()
                        .parseClaimsJws(cleanToken);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("Token validation failed", e);
            return false;
        }
    }

    /**
     * 验证token用于刷新目的（忽略过期时间，只验证签名和用户存在性）
     * @param token
     * @return
     */
    public boolean validateTokenForRefresh(String token) {
        try {
            String cleanToken = cleanToken(token);
            
            // 获取token中的用户名（忽略过期时间）
            String username = getUsernameFromTokenIgnoringExpiration(cleanToken);
            
            // 检查该用户当前有效的token中是否包含此token
            String userTokensKey = "user_tokens:" + username;
            Boolean isMember = redisTemplate.opsForSet().isMember(userTokensKey, cleanToken);
            
            if (Boolean.TRUE.equals(isMember)) {
                // 只验证token签名，忽略过期时间
                Jwts.parserBuilder()
                        .setSigningKey(jwtConfig.getSigningKey())
                        .build()
                        .parseClaimsJws(cleanToken);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("Token validation for refresh failed", e);
            return false;
        }
    }

    /**
     *  清理token字符串，去除前缀和多余空格
     * @param token
     * @return
     */
    private String cleanToken(String token) {
        if (token == null) return null;
        String cleanToken = token.trim();
        if (cleanToken.startsWith("Bearer ")) {
            cleanToken = cleanToken.substring(7).trim();
        }
        return cleanToken;
    }
}