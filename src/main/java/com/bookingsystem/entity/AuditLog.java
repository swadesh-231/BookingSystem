package com.bookingsystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_actor", columnList = "actorEmail"),
        @Index(name = "idx_audit_created", columnList = "createdAt"),
        @Index(name = "idx_audit_target", columnList = "targetEntity, targetId")
})
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(nullable = false)
    private String actorEmail;

    @Column(length = 50)
    private String targetEntity;

    private Long targetId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(length = 45)
    private String ipAddress;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
