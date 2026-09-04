package com.agentguard.payment;

public interface RazorpayOrderClient {
    String createOrder(long amountInPaise, String currency, String receipt);
}
