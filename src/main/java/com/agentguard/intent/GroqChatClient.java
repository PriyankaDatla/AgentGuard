package com.agentguard.intent;

/** Isolates the OpenAI-compatible Groq HTTP contract from intent parsing. */
public interface GroqChatClient {
    String complete(String systemPrompt, String userRequest);
}
