package com.codelab.interfaces.web;

import com.codelab.domain.ExecutionRecord;
import com.codelab.domain.User;
import com.codelab.domain.repository.ExecutionRecordRepository;
import com.codelab.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/result")
@RequiredArgsConstructor
public class ResultController {

    private final ExecutionRecordRepository recordRepository;
    private final UserRepository userRepository;

    @GetMapping("/list")
    public ApiResponse<List<ExecutionRecord>> list(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) return ApiResponse.error(401, "未登录");
        Long userId = userRepository.findByUsername(principal.getUsername()).map(User::getId).orElse(null);
        return ApiResponse.ok(recordRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId));
    }
}


