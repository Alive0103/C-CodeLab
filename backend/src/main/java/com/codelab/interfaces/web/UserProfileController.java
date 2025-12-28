package com.codelab.interfaces.web;

import com.codelab.infrastructure.security.JwtTokenUtils;
import com.codelab.service.UserService;
import com.codelab.domain.User;
import com.codelab.infrastructure.common.ApiResponseCode;
import com.codelab.infrastructure.security.PasswordUtils;
import com.codelab.interfaces.web.dto.ChangePasswordRequest;
import com.codelab.interfaces.web.dto.UpdateProfileRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserService userService;
    private final PasswordUtils passwordUtils;
    private final JwtTokenUtils jwtTokenUtils;
    private final RedisTemplate redisTemplate;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    public ApiResponse<User> getProfile(Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userService.getCurrentUser(username);
        return ApiResponse.ok(currentUser);
    }

    /**
     * 获取当前用户基本信息（简化版）
     */
    @GetMapping
    public ApiResponse<User> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userService.getCurrentUser(username);
        return ApiResponse.ok(currentUser);
    }

    /**
     * 更新用户基本信息
     * 注意：邮箱不允许修改，因为邮箱是账号的唯一标识
     */
    @PutMapping("/profile")
    @Transactional
    public ApiResponse<String> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        String currentUsername = authentication.getName();
        log.info("用户 {} 开始更新个人信息", currentUsername);
        
        User currentUser = userService.getCurrentUser(currentUsername);
        
        // 更新用户名（如果提供了且与当前不同）
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            String newUsername = request.getUsername().trim();
            
            // 如果新用户名与当前用户名相同，无需更新
            if (newUsername.equals(currentUser.getUsername())) {
                log.info("用户名未变化，用户: {}", currentUsername);
                return ApiResponse.ok("更新成功");
            }
            
            // 检查新用户名是否已被使用
            if (userService.existsByUsername(newUsername)) {
                log.warn("用户名已被使用: {}", newUsername);
                return ApiResponse.error(ApiResponseCode.BAD_REQUEST, "用户名已被使用");
            }
            
            log.info("更新用户名: {} -> {}", currentUser.getUsername(), newUsername);
            currentUser.setUsername(newUsername);
            
            // 清除旧用户名的token，强制重新登录
            String oldUserTokensKey = "user_tokens:" + currentUsername;
            redisTemplate.delete(oldUserTokensKey);
            log.info("已清除旧用户名的token: {}", currentUsername);
        }

        userService.updateUser(currentUser);
        log.info("用户信息更新成功，用户: {}", currentUser.getUsername());
        
        return ApiResponse.ok("更新成功，请重新登录");
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    @Transactional
    public ApiResponse<String> changePassword(@Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest, Authentication authentication) {

        String username = authentication.getName();
        log.info("用户 {} 开始修改密码", username);
        
        User currentUser = userService.getCurrentUser(username);
        log.info("获取到用户信息，用户ID: {}", currentUser.getId());

        // 验证旧密码
        log.info("验证旧密码，用户: {}", username);
        boolean isOldPasswordValid = passwordUtils.verifyPassword(request.getOldPassword(),
                currentUser.getPasswordHash());
        log.info("旧密码验证结果: {}, 用户: {}", isOldPasswordValid, username);
        
        if (!isOldPasswordValid) {
            log.warn("旧密码验证失败，用户: {}", username);
            return ApiResponse.error(ApiResponseCode.BAD_REQUEST, "旧密码不正确");
        }

        // 更新密码
        log.info("生成新密码哈希，用户: {}", username);
        String hashedPassword = passwordUtils.hashPassword(request.getNewPassword(), null);
        log.info("新密码哈希生成完成，用户: {}, 哈希长度: {}", username, hashedPassword.length());
        
        currentUser.setPasswordHash(hashedPassword);
        currentUser.setPasswordSalt(""); // BCrypt handles salt internally

        log.info("保存新密码到数据库，用户: {}", username);
        User savedUser = userService.save(currentUser);
        log.info("密码保存完成，用户: {}, 用户ID: {}", username, savedUser.getId());
        
        // 验证保存后的密码是否正确
        log.info("验证保存后的密码，用户: {}", username);
        boolean isNewPasswordValid = passwordUtils.verifyPassword(request.getNewPassword(),
                savedUser.getPasswordHash());
        log.info("新密码验证结果: {}, 用户: {}", isNewPasswordValid, username);
        
        if (!isNewPasswordValid) {
            log.error("密码保存后验证失败，用户: {}", username);
            return ApiResponse.error(ApiResponseCode.INTERNAL_SERVER_ERROR, "密码保存失败，请重试");
        }
        
        // 清除该用户的所有有效token，强制重新登录
        String userTokensKey = "user_tokens:" + username;
        redisTemplate.delete(userTokensKey);
        log.info("已清除用户token，用户: {}", username);
        
        log.info("密码修改成功，用户: {}", username);
        return ApiResponse.ok("密码修改成功");
    }

}
