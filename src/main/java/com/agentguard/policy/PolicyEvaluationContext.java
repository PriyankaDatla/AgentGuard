package com.agentguard.policy;

import com.agentguard.entity.PurchasePolicy;
import com.agentguard.entity.User;
import java.math.BigDecimal;
import java.util.List;

/** Persisted facts supplied to the pure deterministic policy engine. */
public record PolicyEvaluationContext(
        PurchaseIntent intent,
        String merchant,
        BigDecimal requestedAmount,
        User user,
        List<PurchasePolicy> activePolicies,
        BigDecimal hourlySpending,
        boolean duplicateTransaction) {}
