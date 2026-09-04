package com.agentguard.dto;
import com.agentguard.entity.AuditEventType; import java.time.Instant;
public record AuditEventResponse(Long id, Long transactionId, AuditEventType eventType, String description, String metadata, Instant createdAt) {}
