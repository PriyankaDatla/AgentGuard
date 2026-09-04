package com.agentguard.payment;

import com.agentguard.dto.PaymentOrderResponse;
import com.agentguard.entity.*;
import com.agentguard.exception.*;
import com.agentguard.repository.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RazorpayPaymentServiceTest {
    @Mock TransactionRepository transactions;
    @Mock AuditEventRepository auditEvents;
    @Mock RazorpayOrderClient razorpayOrders;
    @InjectMocks RazorpayPaymentService service;

    @BeforeEach void setup() { MockitoAnnotations.openMocks(this); }

    @Test void createsOrderForApprovedTransactionUsingPersistedAmount() {
        Transaction transaction = transaction(TransactionStatus.APPROVED, "4299.00");
        when(transactions.findByIdForUpdate(1L)).thenReturn(Optional.of(transaction));
        when(razorpayOrders.createOrder(429900L, "INR", "txn_null")).thenReturn("order_test_123");

        PaymentOrderResponse response = service.createOrder(1L);

        assertThat(response.razorpayOrderId()).isEqualTo("order_test_123");
        assertThat(transaction.getRazorpayOrderId()).isEqualTo("order_test_123");
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PAYMENT_CREATED);
        verify(razorpayOrders).createOrder(429900L, "INR", "txn_null");
        verify(auditEvents).save(argThat(event -> event.getEventType() == AuditEventType.PAYMENT_CREATED));
    }

    @Test void doesNotCallRazorpayForBlockedTransaction() {
        assertDisallowed(TransactionStatus.BLOCKED);
    }

    @Test void doesNotCallRazorpayForManualReviewTransaction() {
        assertDisallowed(TransactionStatus.MANUAL_REVIEW);
    }

    @Test void rejectsMissingTransaction() {
        when(transactions.findByIdForUpdate(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createOrder(99L)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(razorpayOrders);
    }

    @Test void returnsExistingOrderWithoutCallingRazorpayAgain() {
        Transaction transaction = transaction(TransactionStatus.PAYMENT_CREATED, "4299.00");
        transaction.setRazorpayOrderId("order_existing");
        when(transactions.findByIdForUpdate(1L)).thenReturn(Optional.of(transaction));

        PaymentOrderResponse response = service.createOrder(1L);

        assertThat(response.razorpayOrderId()).isEqualTo("order_existing");
        verifyNoInteractions(razorpayOrders);
    }

    @Test void rejectsInvalidAmountBeforeCallingRazorpay() {
        Transaction transaction = transaction(TransactionStatus.APPROVED, "1.001");
        when(transactions.findByIdForUpdate(1L)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> service.createOrder(1L)).isInstanceOf(InvalidPaymentAmountException.class);
        verifyNoInteractions(razorpayOrders);
    }

    @Test void recordsSanitizedFailureWhenRazorpayFails() {
        Transaction transaction = transaction(TransactionStatus.APPROVED, "4299.00");
        when(transactions.findByIdForUpdate(1L)).thenReturn(Optional.of(transaction));
        when(razorpayOrders.createOrder(anyLong(), anyString(), anyString())).thenThrow(new PaymentProcessingException());

        assertThatThrownBy(() -> service.createOrder(1L)).isInstanceOf(PaymentProcessingException.class);

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PAYMENT_FAILED);
        verify(auditEvents).save(argThat(event -> event.getEventType() == AuditEventType.PAYMENT_FAILED && !event.getDescription().contains("key")));
    }

    private void assertDisallowed(TransactionStatus status) {
        when(transactions.findByIdForUpdate(1L)).thenReturn(Optional.of(transaction(status, "4299.00")));
        assertThatThrownBy(() -> service.createOrder(1L)).isInstanceOf(PaymentNotAllowedException.class);
        verifyNoInteractions(razorpayOrders);
    }
    private Transaction transaction(TransactionStatus status, String amount) {
        Transaction transaction = new Transaction();
        transaction.setStatus(status); transaction.setRequestedAmount(new BigDecimal(amount)); return transaction;
    }
}
