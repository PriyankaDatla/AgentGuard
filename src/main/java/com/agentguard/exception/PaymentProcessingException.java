package com.agentguard.exception;

/** Deliberately contains no provider response or credential data. */
public class PaymentProcessingException extends RuntimeException {
    public PaymentProcessingException() { super("Payment order creation failed"); }
}
