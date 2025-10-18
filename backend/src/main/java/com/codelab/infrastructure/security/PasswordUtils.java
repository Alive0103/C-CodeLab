package com.codelab.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
@Slf4j
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
     * 注意：BCrypt 已经内置盐值处理，salt 参数被忽略
     */
    public String hashPassword(String rawPassword, String salt) {
        // BCrypt 已经内置盐值处理，不需要额外添加盐值
        log.info("生成密码哈希，原始密码长度: {}", rawPassword.length());
        String hashed = passwordEncoder.encode(rawPassword);
        log.info("密码哈希生成完成，哈希长度: {}", hashed.length());
        return hashed;
    }
    
    /**
     * 验证密码（兼容旧接口）
     */
    public boolean verifyPassword(String rawPassword, String salt, String hashedPassword) {
        // 为了兼容旧代码，忽略盐值参数
        log.info("验证密码（兼容接口），原始密码长度: {}, 哈希长度: {}", rawPassword.length(), hashedPassword.length());
        return verifyPassword(rawPassword, hashedPassword);
    }
    
    /**
     * 验证密码
     */
    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        // BCrypt 验证时不需要额外处理盐值
        log.info("验证密码，原始密码长度: {}, 哈希长度: {}", rawPassword.length(), hashedPassword.length());
        boolean result = passwordEncoder.matches(rawPassword, hashedPassword);
        log.info("密码验证结果: {}", result);
        return result;
    }
}