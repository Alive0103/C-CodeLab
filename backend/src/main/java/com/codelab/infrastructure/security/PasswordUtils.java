package com.codelab.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PasswordUtils {
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * 生成随机盐
     */
    public String generateSalt() {
        byte[] saltBytes = new byte[24]; // 24字节 = 192位
        secureRandom.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }
    
    /**
     * 使用盐对密码进行哈希
     */
    public String hashPassword(String rawPassword, String salt) {
        String saltedPassword = rawPassword + salt;
        return passwordEncoder.encode(saltedPassword);
    }
    
    /**
     * 验证密码
     */
    public boolean verifyPassword(String rawPassword, String salt, String hashedPassword) {
        String saltedPassword = rawPassword + salt;
        return passwordEncoder.matches(saltedPassword, hashedPassword);
    }
}
