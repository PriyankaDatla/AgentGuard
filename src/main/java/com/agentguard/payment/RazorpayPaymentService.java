package com.agentguard.payment;

import com.agentguard.dto.PaymentOrderResponse;
import com.agentguard.entity.*;
import com.agentguard.exception.*;
import com.agentguard.repository.AuditEventRepository;
import com.agentguard.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class RazorpayPaymentService implements PaymentService {
    private static final String INR = "INR";
    private final TransactionRepository transactions;
    private final AuditEventRepository auditEvents;
    private final RazorpayOrderClient razorpayOrders;

    public RazorpayPaymentService(TransactionRepository transactions, AuditEventRepository auditEvents, RazorpayOrderClient razorpayOrders) {
        this.transactions = transactions; this.auditEvents = auditEvents; this.razorpayOrders = razorpayOrders;
    }

    @Override
    @Transactional(noRollbackFor = PaymentProcessingException.class)
    public PaymentOrderResponse createOrder(Long transactionId) {
        Transaction transaction = transactions.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));

        if (hasText(transaction.getRazorpayOrderId())) {
            return new PaymentOrderResponse(transaction.getId(), transaction.getRazorpayOrderId(), transaction.getStatus());
        }
        if (transaction.getStatus() != TransactionStatus.APPROVED) {
            throw new PaymentNotAllowedException("Only approved transactions can create a payment order");
        }

        long paise = toPaise(transaction.getRequestedAmount());
        try {
            String orderId = razorpayOrders.createOrder(paise, INR, "txn_" + transaction.getId());
            if (!hasText(orderId)) throw new PaymentProcessingException();
            transaction.setRazorpayOrderId(orderId);
            transaction.setStatus(TransactionStatus.PAYMENT_CREATED);
            transactions.save(transaction);
            audit(transaction.getId(), AuditEventType.PAYMENT_CREATED, "Razorpay payment order created.");
            return new PaymentOrderResponse(transaction.getId(), orderId, TransactionStatus.PAYMENT_CREATED);
        } catch (PaymentProcessingException e) {
            paymentFailure(transaction);
            throw e;
        } catch (RuntimeException e) {
            paymentFailure(transaction);
            throw new PaymentProcessingException();
        }
    }

    private long toPaise(BigDecimal rupees) {
        if (rupees == null || rupees.signum() <= 0) throw new InvalidPaymentAmountException("Transaction amount must be greater than zero");
        try { return rupees.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact(); }
        catch (ArithmeticException e) { throw new InvalidPaymentAmountException("Transaction amount must have at most two decimal places"); }
    }
    private void paymentFailure(Transaction transaction) {
        transaction.setStatus(TransactionStatus.PAYMENT_FAILED);
        transactions.save(transaction);
        audit(transaction.getId(), AuditEventType.PAYMENT_FAILED, "Razorpay payment order creation failed. No payment was initiated.");
    }
    private void audit(Long transactionId, AuditEventType type, String description) { AuditEvent event = new AuditEvent(); event.setTransactionId(transactionId); event.setEventType(type); event.setDescription(description); auditEvents.save(event); }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
}
