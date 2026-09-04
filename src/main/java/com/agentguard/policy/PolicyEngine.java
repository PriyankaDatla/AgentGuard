package com.agentguard.policy;

public interface PolicyEngine {
    PolicyDecision evaluate(PolicyEvaluationContext context);
}
