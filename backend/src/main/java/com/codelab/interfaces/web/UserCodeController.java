package com.codelab.interfaces.web;

import com.codelab.application.CodeSnippetService;
import com.codelab.domain.CodeSnippet;
import com.codelab.domain.ExecutionRecord;
import com.codelab.domain.User;
import com.codelab.domain.repository.ExecutionRecordRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserCodeController {

    private final CodeSnippetService codeSnippetService;
    private final ExecutionRecordRepository executionRecordRepository;

    /**
     * 获取当前用户的代码片段列表
     */
    @GetMapping("/code-snippets")
    public ApiResponse<List<CodeSnippet>> getMyCodeSnippets(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ApiResponse.error(401, "未登录");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<CodeSnippet> snippets = codeSnippetService.listRecent(user.getId());
        return ApiResponse.ok(snippets);
    }

    /**
     * 获取当前用户的执行记录
     */
    @GetMapping("/execution-records")
    public ApiResponse<Page<ExecutionRecord>> getMyExecutionRecords(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ApiResponse.error(401, "未登录");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ExecutionRecord> records = executionRecordRepository.findByUserId(user.getId(), pageable);
        return ApiResponse.ok(records);
    }

    /**
     * 删除代码片段
     */
    @DeleteMapping("/code-snippets/{id}")
    public ApiResponse<String> deleteCodeSnippet(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ApiResponse.error(401, "未登录");
        }

        // 这里需要添加删除逻辑，确保只能删除自己的代码片段
        return ApiResponse.ok("删除成功");
    }

    /**
     * 删除执行记录
     */
    @DeleteMapping("/execution-records/{id}")
    public ApiResponse<String> deleteExecutionRecord(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ApiResponse.error(401, "未登录");
        }

        // 这里需要添加删除逻辑，确保只能删除自己的执行记录
        return ApiResponse.ok("删除成功");
    }
}
