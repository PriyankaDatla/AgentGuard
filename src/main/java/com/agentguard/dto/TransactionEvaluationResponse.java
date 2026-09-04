package com.agentguard.dto;

import com.agentguard.policy.PolicyDecisionStatus;
import java.util.List;

public record TransactionEvaluationResponse(
        Long transactionId,
        PolicyDecisionStatus decision,
        int riskScore,
        List<String> reasons,
        List<PolicyCheckResponse> policyChecks) {}
