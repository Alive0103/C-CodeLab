package com.codelab.interfaces.web;

import com.codelab.service.UserService;
import com.codelab.domain.User;
import com.codelab.application.AuthService;
import com.codelab.interfaces.web.dto.LoginRequest;
import com.codelab.interfaces.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/auth/register")
    public ApiResponse<String> register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req.getUsername(), req.getPassword(), req.getEmail());
    }

    @PostMapping("/auth/login")
    public ApiResponse<String> login(@Valid @RequestBody LoginRequest req) {
        return authService.authenticate(req.getUsername(), req.getPassword());
    }

    @PostMapping("/auth/logout")
    public ApiResponse<String> logout() {
        return ApiResponse.ok("登出成功");
    }
}


