package com.classroom.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TokenSessionService {

    private static final String TOKEN_PREFIX = "auth:token:";

    private final StringRedisTemplate redisTemplate;

    public TokenSessionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void storeToken(String token, Long userId, long ttlMillis) {
        if (token == null || token.isBlank() || userId == null || ttlMillis <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(buildTokenKey(token), String.valueOf(userId), Duration.ofMillis(ttlMillis));
    }

    public boolean isTokenValid(String token, Long userId) {
        if (token == null || token.isBlank() || userId == null) {
            return false;
        }
        String cachedUserId = redisTemplate.opsForValue().get(buildTokenKey(token));
        return String.valueOf(userId).equals(cachedUserId);
    }

    public void revokeToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        redisTemplate.delete(buildTokenKey(token));
    }

    private String buildTokenKey(String token) {
        return TOKEN_PREFIX + token;
    }
}

