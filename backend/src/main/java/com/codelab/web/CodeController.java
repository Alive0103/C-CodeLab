package com.codelab.web;

import com.codelab.domain.CodeSnippet;
import com.codelab.domain.User;
import com.codelab.repository.UserRepository;
import com.codelab.service.CodeExecutionService;
import com.codelab.service.CodeSnippetService;
import com.codelab.web.dto.RunCodeRequest;
import com.codelab.web.dto.SaveCodeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/code")
@RequiredArgsConstructor
public class CodeController {

    private final CodeSnippetService snippetService;
    private final CodeExecutionService executionService;
    private final UserRepository userRepository;

    @PostMapping("/save")
    public ApiResponse<?> save(@Valid @RequestBody SaveCodeRequest req,
                               @AuthenticationPrincipal UserDetails principal) {
        Long userId = principal != null ? userRepository.findByUsername(principal.getUsername()).map(User::getId).orElse(null) : null;
        CodeSnippet s = snippetService.saveSnippet(userId, req.getTitle(), req.getCodeContent(), req.getLanguage(), req.isPublic());
        recordIdOnly r = new recordIdOnly(s.getId(), s.getTitle(), s.getCreatedAt().toString());
        return ApiResponse.ok(r);
    }

    @PostMapping("/run")
    public ApiResponse<?> run(@Valid @RequestBody RunCodeRequest req,
                              @AuthenticationPrincipal UserDetails principal) {
        Long userId = principal != null ? userRepository.findByUsername(principal.getUsername()).map(User::getId).orElse(null) : null;
        return ApiResponse.ok(executionService.compileAndRun(req.getCode(), userId, req.getTitle()));
    }

    private record recordIdOnly(Long id, String title, String createdAt) {}
}


