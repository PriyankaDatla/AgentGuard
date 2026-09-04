package com.agentguard.dto;

import com.agentguard.entity.TransactionStatus;

public record PaymentOrderResponse(Long transactionId, String razorpayOrderId, TransactionStatus status) {}
