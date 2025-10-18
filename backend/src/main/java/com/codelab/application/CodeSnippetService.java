package com.codelab.application;

import com.codelab.domain.CodeSnippet;
import com.codelab.domain.User;
import com.codelab.domain.repository.CodeSnippetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CodeSnippetService {

    private final CodeSnippetRepository snippetRepository;

    @Transactional
    public CodeSnippet saveSnippet(Long userId, String title, String codeContent, String language, boolean isPublic) {
        snippetRepository.findByUserIdAndTitle(userId, title).ifPresent(s -> {
            throw new DuplicateKeyException("标题已存在，请修改");
        });
        CodeSnippet snippet = new CodeSnippet();
        User user = new User();
        user.setId(userId);
        snippet.setUser(user);
        snippet.setTitle(title);
        snippet.setCodeContent(codeContent);
        snippet.setLanguage(language);
        snippet.setIsPublic(isPublic);
        return snippetRepository.save(snippet);
    }

    public List<CodeSnippet> listRecent(Long userId) {
        return snippetRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }
}


