package com.codelab.interfaces.web;

import com.codelab.application.CodeSnippetService;
import com.codelab.service.UserService;
import com.codelab.domain.CodeSnippet;
import com.codelab.domain.ExecutionRecord;
import com.codelab.domain.User;
import com.codelab.domain.repository.ExecutionRecordRepository;
import com.codelab.infrastructure.common.ApiResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserCodeController {

    private final CodeSnippetService codeSnippetService;
    private final ExecutionRecordRepository executionRecordRepository;
    private final UserService userService;

    /**
     * 从SecurityContext获取当前用户名
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        throw new IllegalStateException("用户未认证");
    }

    /**
     * 获取当前用户的代码片段列表
     */
    @GetMapping("/code-snippets")
    public ApiResponse<Page<CodeSnippet>> getMyCodeSnippets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String username = getCurrentUsername();
        User user = userService.getCurrentUser(username);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        // 使用Pageable进行分页查询
        Page<CodeSnippet> snippets = codeSnippetService.listRecent(user.getId(), pageable);
        return ApiResponse.ok(snippets);
    }

    /**
     * 获取当前用户的执行记录
     */
    @GetMapping("/execution-records")
    public ApiResponse<Page<ExecutionRecord>> getMyExecutionRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String username = getCurrentUsername();
        User user = userService.getCurrentUser(username);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ExecutionRecord> records = executionRecordRepository.findByUserId(user.getId(), pageable);
        return ApiResponse.ok(records);
    }

    /**
     * 删除代码片段
     */
    @DeleteMapping("/code-snippets/{id}")
    public ApiResponse<String> deleteCodeSnippet(@PathVariable Long id) {
        String username = getCurrentUsername();
        User user = userService.getCurrentUser(username);
        
        // 验证代码片段是否属于当前用户
        CodeSnippet snippet = codeSnippetService.findById(id);
        if (snippet == null) {
            return ApiResponse.error(ApiResponseCode.BAD_REQUEST, "代码片段不存在");
        }
        if (!snippet.getUser().getId().equals(user.getId())) {
            return ApiResponse.error(ApiResponseCode.FORBIDDEN, "无权限删除此代码片段");
        }
        
        codeSnippetService.deleteById(id);
        return ApiResponse.ok("删除成功");
    }

    /**
     * 删除执行记录
     */
    @DeleteMapping("/execution-records/{id}")
    public ApiResponse<String> deleteExecutionRecord(@PathVariable Long id) {
        String username = getCurrentUsername();
        User user = userService.getCurrentUser(username);
        
        // 验证执行记录是否属于当前用户
        ExecutionRecord record = executionRecordRepository.findById(id).orElse(null);
        if (record == null) {
            return ApiResponse.error(ApiResponseCode.BAD_REQUEST, "执行记录不存在");
        }
        if (!record.getUser().getId().equals(user.getId())) {
            return ApiResponse.error(ApiResponseCode.FORBIDDEN, "无权限删除此执行记录");
        }
        
        executionRecordRepository.deleteById(id);
        return ApiResponse.ok("删除成功");
    }
}