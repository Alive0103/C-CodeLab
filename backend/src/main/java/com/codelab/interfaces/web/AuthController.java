package com.codelab.interfaces.web;

import com.codelab.domain.User;
import com.codelab.application.AuthService;
import com.codelab.interfaces.web.dto.LoginRequest;
import com.codelab.interfaces.web.dto.RegisterRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/register")
    public ApiResponse<String> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req.getUsername(), req.getPassword(), req.getEmail());
        return ApiResponse.ok("注册成功");
    }

    @PostMapping("/auth/login")
    public ApiResponse<User> login(@Valid @RequestBody LoginRequest req, HttpSession session) {
        User user = authService.authenticate(req.getUsername(), req.getPassword());
        session.setAttribute("user", user);
        return ApiResponse.ok(user);
    }

    @PostMapping("/auth/logout")
    public ApiResponse<String> logout(HttpSession session) {
        session.invalidate();
        return ApiResponse.ok("登出成功");
    }

    @GetMapping("/user")
    public ApiResponse<?> currentUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ApiResponse.error(401, "未登录");
        }
        return ApiResponse.ok(user);
    }
}


