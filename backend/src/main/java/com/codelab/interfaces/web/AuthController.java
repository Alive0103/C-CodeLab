package com.codelab.interfaces.web;

import com.codelab.domain.User;
import com.codelab.domain.repository.UserRepository;
import com.codelab.application.AuthService;
import com.codelab.interfaces.web.dto.LoginRequest;
import com.codelab.interfaces.web.dto.RegisterRequest;
import com.codelab.interfaces.web.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/auth/register")
    public ApiResponse<TokenResponse> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req.getUsername(), req.getPassword(), req.getEmail());
        String access = authService.loginAndIssueAccessToken(req.getUsername(), req.getPassword());
        TokenResponse resp = new TokenResponse();
        resp.setAccessToken(access);
        return ApiResponse.ok(resp);
    }

    @PostMapping("/auth/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        String access = authService.loginAndIssueAccessToken(req.getUsername(), req.getPassword());
        TokenResponse resp = new TokenResponse();
        resp.setAccessToken(access);
        return ApiResponse.ok(resp);
    }

    @GetMapping("/user")
    public ApiResponse<?> currentUser(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) return ApiResponse.error(401, "未登录");
        User u = userRepository.findByUsername(principal.getUsername()).orElse(null);
        return ApiResponse.ok(u);
    }
}


