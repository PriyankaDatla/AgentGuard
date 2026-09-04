package com.agentguard.dto;

public record PolicyCheckResponse(String rule, boolean passed, String explanation) {}
