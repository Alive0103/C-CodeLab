package com.codelab.interfaces.web;

import com.codelab.service.UserService;
import com.codelab.domain.CodeSnippet;
import com.codelab.domain.User;
import com.codelab.application.CodeExecutionService;
import com.codelab.application.CodeSnippetService;
import com.codelab.interfaces.web.dto.RunCodeRequest;
import com.codelab.interfaces.web.dto.SaveCodeRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/code")
@RequiredArgsConstructor
public class CodeController {

    private final CodeSnippetService snippetService;
    private final CodeExecutionService executionService;
    private final UserService userService;

    @PostMapping("/save")
    public ApiResponse<?> save(@Valid @RequestBody SaveCodeRequest req, HttpServletRequest request, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getCurrentUser(username);
        CodeSnippet s = snippetService.saveSnippet(user.getId(), req.getTitle(), req.getCodeContent(), req.getLanguage(), req.isPublic());
        RecordIdOnly r = new RecordIdOnly(s.getId(), s.getTitle(), s.getCreatedAt().toString());
        return ApiResponse.ok(r);
    }

    @PostMapping("/run")
    public ApiResponse<?> run(@Valid @RequestBody RunCodeRequest req, HttpServletRequest request, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getCurrentUser(username);
        // 直接同步执行，避免异步安全上下文问题
        CodeExecutionService.ExecutionResult result = executionService.compileAndRun(req.getCode(), user.getId(), req.getTitle());
        return ApiResponse.ok(result);
    }

    private static class RecordIdOnly {
        private final Long id;
        private final String title;
        private final String createdAt;
        
        public RecordIdOnly(Long id, String title, String createdAt) {
            this.id = id;
            this.title = title;
            this.createdAt = createdAt;
        }
        
        public Long getId() { return id; }
        public String getTitle() { return title; }
        public String getCreatedAt() { return createdAt; }
    }
}


