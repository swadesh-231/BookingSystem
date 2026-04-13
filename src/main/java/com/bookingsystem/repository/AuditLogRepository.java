package com.bookingsystem.repository;

import com.bookingsystem.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByActionContainingIgnoreCaseOrderByCreatedAtDesc(String action, Pageable pageable);
    Page<AuditLog> findByActorEmailOrderByCreatedAtDesc(String actorEmail, Pageable pageable);
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
