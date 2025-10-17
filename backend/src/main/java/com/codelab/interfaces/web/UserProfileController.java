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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
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
     * 更新用户基本信息
     */
    @PutMapping("/profile")
    public ApiResponse<String> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userService.getCurrentUser(username);
        // 更新邮箱（如果提供了且不重复）
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            if (!request.getEmail().equals(currentUser.getEmail()) && 
                userService.existsByEmail(request.getEmail())) {
                return ApiResponse.error(ApiResponseCode.BAD_REQUEST, "邮箱已被使用");
            }
            currentUser.setEmail(request.getEmail());
        }

        userService.updateUser(currentUser);
        
        return ApiResponse.ok("更新成功");
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public ApiResponse<String> changePassword(@Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest, Authentication authentication) {

        String username = authentication.getName();
        User currentUser = userService.getCurrentUser(username);

        // 验证旧密码
        if (!passwordUtils.verifyPassword(request.getOldPassword(),
                currentUser.getPasswordSalt(), currentUser.getPasswordHash())) {
            return ApiResponse.error(ApiResponseCode.BAD_REQUEST, "旧密码不正确");
        }

        // 更新密码
        String salt = passwordUtils.generateSalt();
        String hashedPassword = passwordUtils.hashPassword(request.getNewPassword(), salt);
        currentUser.setPasswordHash(hashedPassword);
        currentUser.setPasswordSalt(salt);

        userService.save(currentUser);
        // 清除该用户的所有有效token，强制重新登录
        String userTokensKey = "user_tokens:" + username;
        redisTemplate.delete(userTokensKey);
        return ApiResponse.ok("密码修改成功");
    }

}
