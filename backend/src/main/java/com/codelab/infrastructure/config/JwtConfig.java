package com.codelab.infrastructure.config;


import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private Long expiration = 86400000L;
    private String secret ="mySecretKey";

    public SecretKey getSigningKey() {
        if (secret == null || secret.isEmpty()) {
            // 生成安全密钥
            return Keys.secretKeyFor(SignatureAlgorithm.HS512);
        }

        // 如果密钥长度不足，使用 SHA-512 扩展
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-512");
                keyBytes = digest.digest(keyBytes);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Failed to create secure key", e);
            }
        }
        return new SecretKeySpec(keyBytes, SignatureAlgorithm.HS512.getJcaName());
    }
}
