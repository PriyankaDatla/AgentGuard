package com.agentguard.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionTest {
    @Test
    void tracksRazorpayIdentifiersSeparately() {
        Transaction transaction = new Transaction();

        transaction.setRazorpayOrderId("order_test_123");
        transaction.setRazorpayPaymentId("pay_test_456");

        assertThat(transaction.getRazorpayOrderId()).isEqualTo("order_test_123");
        assertThat(transaction.getRazorpayPaymentId()).isEqualTo("pay_test_456");
    }
}
