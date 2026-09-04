package com.agentguard.service;

import com.agentguard.dto.*;
import com.agentguard.intent.IntentService;
import com.agentguard.policy.PurchaseIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AgentEvaluationService {
    private static final Logger log = LoggerFactory.getLogger(AgentEvaluationService.class);
    private final IntentService intents;
    private final PolicyEvaluationService policies;
    public AgentEvaluationService(IntentService intents, PolicyEvaluationService policies) { this.intents = intents; this.policies = policies; }
    public AgentEvaluateResponse evaluate(AgentEvaluateRequest request) {
        log.info("Agent evaluation started for userId={}", request.userId());
        PurchaseIntent intent = intents.parseIntent(request.request(), request.userId());
        TransactionEvaluationResponse decision = policies.evaluate(new EvaluateTransactionRequest(request.userId(), request.merchant(), intent.category(), request.amount()), intent);
        return new AgentEvaluateResponse(intent, decision);
    }
}
