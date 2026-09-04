package com.agentguard.policy;

import java.util.List;

public record PolicyDecision(
        PolicyDecisionStatus decision,
        int riskScore,
        List<String> reasons,
        List<PolicyCheck> policyChecks) {}
