package com.codelab.interfaces.web;

import com.codelab.service.UserService;
import com.codelab.domain.ExecutionRecord;
import com.codelab.domain.User;
import com.codelab.domain.repository.ExecutionRecordRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/result")
@RequiredArgsConstructor
public class ResultController {

    private final ExecutionRecordRepository recordRepository;
    private final UserService userService;

    @GetMapping("/list")
    public ApiResponse<List<ExecutionRecord>> list(HttpServletRequest request, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getCurrentUser(username);
        return ApiResponse.ok(recordRepository.findTop50ByUserIdOrderByCreatedAtDesc(user.getId()));
    }
}


