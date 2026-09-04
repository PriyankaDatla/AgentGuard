package com.agentguard.dto;
import java.math.BigDecimal; import java.time.Instant;
public record PolicyResponse(Long id, Long userId, String category, BigDecimal maxAmount, boolean requiresApproval, boolean active, Instant createdAt) {}
