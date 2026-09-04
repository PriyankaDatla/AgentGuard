package com.agentguard.dto;
import java.math.BigDecimal; import java.time.Instant;
public record UserResponse(Long id, String name, String email, BigDecimal spendingLimit, BigDecimal hourlySpendingLimit, Instant createdAt) {}
