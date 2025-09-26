package com.codelab.domain.repository;

import com.codelab.domain.CodeSnippet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodeSnippetRepository extends JpaRepository<CodeSnippet, Long> {
    Optional<CodeSnippet> findByUserIdAndTitle(Long userId, String title);
    List<CodeSnippet> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);
}


