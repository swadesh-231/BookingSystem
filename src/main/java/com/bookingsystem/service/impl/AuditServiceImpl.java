package com.bookingsystem.service.impl;

import com.bookingsystem.entity.AuditLog;
import com.bookingsystem.entity.User;
import com.bookingsystem.repository.AuditLogRepository;
import com.bookingsystem.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {
    private final AuditLogRepository auditLogRepository;

    @Override
    @Async("pricingExecutor")
    public void log(String action, String targetEntity, Long targetId, String details) {
        try {
            String actorEmail = resolveActorEmail();
            String ipAddress = resolveIpAddress();

            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .actorEmail(actorEmail)
                    .targetEntity(targetEntity)
                    .targetId(targetId)
                    .details(details)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("AUDIT: {} by {} on {}:{} - {}", action, actorEmail, targetEntity, targetId, details);
        } catch (Exception e) {
            // Audit logging should never break the main flow
            log.error("Failed to write audit log: {}", e.getMessage());
        }
    }

    private String resolveActorEmail() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User user) {
                return user.getEmail();
            }
        } catch (Exception ignored) {}
        return "SYSTEM";
    }

    private String resolveIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
