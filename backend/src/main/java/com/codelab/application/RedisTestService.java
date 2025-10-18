package com.codelab.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisTestService {

    private final RedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    public void testRedisConnection() {
        try {
            // 测试Redis连接
            redisTemplate.opsForValue().set("test-key", "test-value");
            String value = (String) redisTemplate.opsForValue().get("test-key");
            
            if ("test-value".equals(value)) {
                log.info("Redis连接测试成功，读写正常");
            } else {
                log.error("Redis连接测试失败，无法正确读取写入的值");
            }
            
            // 清理测试数据
            redisTemplate.delete("test-key");
        } catch (Exception e) {
            log.error("Redis连接测试失败: " + e.getMessage(), e);
        }
    }
}