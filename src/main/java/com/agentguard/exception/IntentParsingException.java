package com.agentguard.exception;

/** The provider output cannot be safely converted to a constrained purchase intent. */
public class IntentParsingException extends RuntimeException {
    public IntentParsingException(String message) { super(message); }
    public IntentParsingException(String message, Throwable cause) { super(message, cause); }
}
