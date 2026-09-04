package com.agentguard.intent;

import com.agentguard.policy.PurchaseIntent;

public interface IntentService {
    PurchaseIntent parseIntent(String userRequest, Long userId);
}
