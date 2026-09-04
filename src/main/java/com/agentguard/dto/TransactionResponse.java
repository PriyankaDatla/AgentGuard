package com.agentguard.dto;
import com.agentguard.entity.TransactionStatus; import java.math.BigDecimal; import java.time.Instant;
public record TransactionResponse(Long id, Long userId, String merchant, String category, BigDecimal requestedAmount, BigDecimal actualAmount, TransactionStatus status, Integer riskScore, String decisionReason, Instant createdAt) {}
