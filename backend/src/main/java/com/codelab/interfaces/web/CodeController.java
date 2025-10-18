package com.codelab.interfaces.web;

import com.codelab.domain.CodeSnippet;
import com.codelab.domain.User;
import com.codelab.application.CodeExecutionService;
import com.codelab.application.CodeSnippetService;
import com.codelab.interfaces.web.dto.RunCodeRequest;
import com.codelab.interfaces.web.dto.SaveCodeRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/code")
@RequiredArgsConstructor
public class CodeController {

    private final CodeSnippetService snippetService;
    private final CodeExecutionService executionService;

    @PostMapping("/save")
    public ApiResponse<?> save(@Valid @RequestBody SaveCodeRequest req, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ApiResponse.error(401, "未登录");
        }
        CodeSnippet s = snippetService.saveSnippet(user.getId(), req.getTitle(), req.getCodeContent(), req.getLanguage(), req.isPublic());
        RecordIdOnly r = new RecordIdOnly(s.getId(), s.getTitle(), s.getCreatedAt().toString());
        return ApiResponse.ok(r);
    }

    @PostMapping("/run")
    public ApiResponse<?> run(@Valid @RequestBody RunCodeRequest req, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ApiResponse.error(401, "未登录");
        }
        try {
            // 等待异步执行完成并返回结果
            var result = executionService.compileAndRun(req.getCode(), user.getId(), req.getTitle()).get();
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error(500, "代码执行失败：" + e.getMessage());
        }
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


