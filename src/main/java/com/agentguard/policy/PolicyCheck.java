package com.agentguard.policy;

public record PolicyCheck(String rule, boolean passed, String explanation) {}
