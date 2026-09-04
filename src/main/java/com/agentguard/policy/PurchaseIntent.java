package com.agentguard.policy;

import java.math.BigDecimal;

/** Structured, untrusted input that a future LLM layer may produce. */
public record PurchaseIntent(
        Long userId,
        String category,
        String preferredBrand,
        BigDecimal maxAmount,
        boolean requiresCategoryMatch) {}
