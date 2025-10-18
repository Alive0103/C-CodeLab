package com.codelab.interfaces.web;

import com.codelab.domain.User;
import com.codelab.domain.repository.UserRepository;
import com.codelab.infrastructure.security.PasswordUtils;
import com.codelab.interfaces.web.dto.ChangePasswordRequest;
import com.codelab.interfaces.web.dto.UpdateProfileRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserRepository userRepository;
    private final PasswordUtils passwordUtils;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    public ApiResponse<User> getProfile(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ApiResponse.error(401, "未登录");
        }
        return ApiResponse.ok(user);
    }

    /**
     * 更新用户基本信息
     */
    @PutMapping("/profile")
    public ApiResponse<String> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ApiResponse.error(401, "未登录");
        }

        // 获取最新的用户信息
        Optional<User> userOpt = userRepository.findById(user.getId());
        if (userOpt.isEmpty()) {
            return ApiResponse.error(404, "用户不存在");
        }

        User currentUser = userOpt.get();
        
        // 更新邮箱（如果提供了且不重复）
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            if (!request.getEmail().equals(currentUser.getEmail()) && 
                userRepository.existsByEmail(request.getEmail())) {
                return ApiResponse.error(400, "邮箱已被使用");
            }
            currentUser.setEmail(request.getEmail());
        }

        userRepository.save(currentUser);
        
        // 更新session中的用户信息
        session.setAttribute("user", currentUser);
        
        return ApiResponse.ok("更新成功");
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public ApiResponse<String> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ApiResponse.error(401, "未登录");
        }

        // 获取最新的用户信息
        Optional<User> userOpt = userRepository.findById(user.getId());
        if (userOpt.isEmpty()) {
            return ApiResponse.error(404, "用户不存在");
        }

        User currentUser = userOpt.get();
        
        // 验证旧密码
        if (!passwordUtils.verifyPassword(request.getOldPassword(), 
                currentUser.getPasswordSalt(), currentUser.getPasswordHash())) {
            return ApiResponse.error(400, "原密码错误");
        }

        // 更新密码
        String salt = passwordUtils.generateSalt();
        String hashedPassword = passwordUtils.hashPassword(request.getNewPassword(), salt);
        currentUser.setPasswordHash(hashedPassword);
        currentUser.setPasswordSalt(salt);

        userRepository.save(currentUser);
        
        return ApiResponse.ok("密码修改成功");
    }
}
