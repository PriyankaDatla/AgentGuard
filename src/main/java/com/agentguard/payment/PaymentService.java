package com.agentguard.payment;

import com.agentguard.dto.PaymentOrderResponse;

public interface PaymentService {
    PaymentOrderResponse createOrder(Long transactionId);
}
