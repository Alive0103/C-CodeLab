package com.codelab.domain.repository;

import com.codelab.domain.ExecutionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExecutionRecordRepository extends JpaRepository<ExecutionRecord, Long> {
    List<ExecutionRecord> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);
    Page<ExecutionRecord> findByUserId(Long userId, Pageable pageable);
}


