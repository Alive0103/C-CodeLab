package com.codelab.application;

import com.codelab.domain.User;
import com.codelab.infrastructure.common.ApiResponseCode;
import com.codelab.infrastructure.config.JwtConfig;
import com.codelab.infrastructure.security.JwtTokenUtils;
import com.codelab.infrastructure.security.PasswordUtils;
import com.codelab.interfaces.web.ApiResponse;
import com.codelab.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserService userService;
    private final PasswordUtils passwordUtils;
    private final JwtTokenUtils jwtTokenUtils;
    private final RedisTemplate redisTemplate;
    private final JwtConfig jwtConfig;

    @Transactional
    public ApiResponse<String> register(String username, String rawPassword, String email) {
        if (userService.existsByUsername(username)) {
            return ApiResponse.error(ApiResponseCode.BAD_REQUEST, "用户名已被使用");
        }
        
        String salt = passwordUtils.generateSalt();
        String hashedPassword = passwordUtils.hashPassword(rawPassword, salt);
        
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(hashedPassword);
        u.setPasswordSalt(salt);
        u.setEmail(email);
        u.setRole("ROLE_USER");
        u.setEnabled(true);
        userService.save(u);

        log.info("用户注册成功: {}", username);
        return ApiResponse.ok("注册成功");
    }

    public ApiResponse<String> authenticate(String username, String password) {
        log.info("开始认证用户: {}", username);

        Optional<User> userOpt = userService.findByUsername(username);
        if (userOpt.isEmpty()) {
            log.warn("用户不存在: {}", username);
            return ApiResponse.error(ApiResponseCode.UNAUTHORIZED, "用户名或密码错误");
        }
        
        User user = userOpt.get();
        log.info("找到用户: {}, enabled: {}", username, user.getEnabled());
        if (!user.getEnabled()) {
            log.warn("用户账户已被禁用: {}", username);
            return ApiResponse.error(ApiResponseCode.FORBIDDEN, "用户账户已被禁用");
        }
        
        log.info("验证密码，用户: {}", username);
        boolean isValid = passwordUtils.verifyPassword(password, user.getPasswordHash());
        log.info("密码验证结果: {}, 用户: {}", isValid, username);
        
        // 修改验证方式，不再传递盐值参数
        if (!isValid) {
            log.warn("密码验证失败，用户: {}", username);
            return ApiResponse.error(ApiResponseCode.UNAUTHORIZED, "用户名或密码错误");
        }
        // 清除该用户的所有有效token，强制重新登录
        String userTokensKey = "user_tokens:" + username;
        redisTemplate.delete(userTokensKey);

        String cleanToken = jwtTokenUtils.generateToken(username);
        String token = "Bearer " + cleanToken;
        redisTemplate.opsForSet().add(userTokensKey, cleanToken);
        // 设置过期时间与token一致
        redisTemplate.expire(userTokensKey, jwtConfig.getExpiration(), TimeUnit.MILLISECONDS);

        log.info("用户认证成功: {}", username);
        return ApiResponse.ok(token);
    }
}