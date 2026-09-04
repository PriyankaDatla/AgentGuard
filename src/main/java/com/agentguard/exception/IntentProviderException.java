package com.agentguard.exception;

/** The configured intent provider could not reliably serve the request. */
public class IntentProviderException extends RuntimeException {
    public IntentProviderException(String message, Throwable cause) { super(message, cause); }
    public IntentProviderException(String message) { super(message); }
}
