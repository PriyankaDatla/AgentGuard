package com.agentguard.dto;

import com.agentguard.policy.PurchaseIntent;

public record AgentEvaluateResponse(PurchaseIntent intent, TransactionEvaluationResponse decision) {}
