package com.agentguard.service;

import com.agentguard.dto.*;
import com.agentguard.entity.*;
import com.agentguard.exception.ResourceNotFoundException;
import com.agentguard.policy.*;
import com.agentguard.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class PolicyEvaluationService {
    private static final List<TransactionStatus> HOURLY_SPENDING_STATUSES = List.of(TransactionStatus.APPROVED, TransactionStatus.PAYMENT_CREATED, TransactionStatus.COMPLETED);
    private final UserRepository users;
    private final PurchasePolicyRepository policies;
    private final TransactionRepository transactions;
    private final AuditEventRepository auditEvents;
    private final PolicyEngine policyEngine;

    public PolicyEvaluationService(UserRepository users, PurchasePolicyRepository policies, TransactionRepository transactions, AuditEventRepository auditEvents, PolicyEngine policyEngine) {
        this.users = users; this.policies = policies; this.transactions = transactions; this.auditEvents = auditEvents; this.policyEngine = policyEngine;
    }

    @Transactional
    public TransactionEvaluationResponse evaluate(EvaluateTransactionRequest request) {
        return evaluate(request, new PurchaseIntent(request.userId(), request.category(), null, request.requestedAmount(), true));
    }

    @Transactional
    public TransactionEvaluationResponse evaluate(EvaluateTransactionRequest request, PurchaseIntent intent) {
        if (!request.userId().equals(intent.userId())) throw new IllegalArgumentException("Intent user does not match transaction user");
        User user = users.findById(request.userId()).orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userId()));
        Instant now = Instant.now();
        List<PurchasePolicy> activePolicies = policies.findByUserIdOrderByCreatedAtDesc(request.userId()).stream().filter(PurchasePolicy::isActive).toList();
        BigDecimal hourlySpending = transactions.sumSpendingSince(request.userId(), now.minus(60, ChronoUnit.MINUTES), HOURLY_SPENDING_STATUSES);
        boolean duplicate = transactions.existsByUserIdAndMerchantIgnoreCaseAndCategoryIgnoreCaseAndRequestedAmountAndCreatedAtAfter(request.userId(), request.merchant(), request.category(), request.requestedAmount(), now.minus(10, ChronoUnit.MINUTES));
        PolicyDecision decision = policyEngine.evaluate(new PolicyEvaluationContext(intent, request.merchant(), request.requestedAmount(), user, activePolicies, hourlySpending == null ? BigDecimal.ZERO : hourlySpending, duplicate));
        Transaction transaction = new Transaction();
        transaction.setUserId(request.userId()); transaction.setMerchant(request.merchant().trim()); transaction.setCategory(request.category().trim()); transaction.setRequestedAmount(request.requestedAmount());
        transaction.setStatus(toTransactionStatus(decision.decision())); transaction.setRiskScore(decision.riskScore()); transaction.setDecisionReason(String.join(" ", decision.reasons()));
        transaction = transactions.save(transaction);
        recordAuditEvents(transaction, decision);
        return new TransactionEvaluationResponse(transaction.getId(), decision.decision(), decision.riskScore(), decision.reasons(), decision.policyChecks().stream().map(c -> new PolicyCheckResponse(c.rule(), c.passed(), c.explanation())).toList());
    }

    private TransactionStatus toTransactionStatus(PolicyDecisionStatus decision) { return switch (decision) { case APPROVED -> TransactionStatus.APPROVED; case BLOCKED -> TransactionStatus.BLOCKED; case MANUAL_REVIEW -> TransactionStatus.MANUAL_REVIEW; }; }
    private void recordAuditEvents(Transaction transaction, PolicyDecision decision) {
        saveAudit(transaction.getId(), AuditEventType.TRANSACTION_CREATED, "Transaction evaluation request created.");
        saveAudit(transaction.getId(), AuditEventType.POLICY_EVALUATED, "Policy engine decision: " + decision.decision() + ". " + String.join(" ", decision.reasons()));
        AuditEventType terminal = switch (decision.decision()) { case APPROVED -> AuditEventType.TRANSACTION_APPROVED; case BLOCKED -> AuditEventType.TRANSACTION_BLOCKED; case MANUAL_REVIEW -> AuditEventType.TRANSACTION_MANUAL_REVIEW; };
        saveAudit(transaction.getId(), terminal, "Transaction " + decision.decision() + ". " + String.join(" ", decision.reasons()));
    }
    private void saveAudit(Long transactionId, AuditEventType type, String description) { AuditEvent event = new AuditEvent(); event.setTransactionId(transactionId); event.setEventType(type); event.setDescription(description); auditEvents.save(event); }
}
