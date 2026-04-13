package com.bookingsystem.service;

public interface AuditService {
    void log(String action, String targetEntity, Long targetId, String details);
}
