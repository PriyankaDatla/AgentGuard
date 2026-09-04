package com.agentguard.policy;

import com.agentguard.entity.PurchasePolicy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class DeterministicPolicyEngine implements PolicyEngine {
    private static final BigDecimal EIGHTY_PERCENT = new BigDecimal("0.80");

    @Override
    public PolicyDecision evaluate(PolicyEvaluationContext context) {
        List<PolicyCheck> checks = new ArrayList<>();
        List<String> reasons = new ArrayList<>();

        if (invalid(context)) {
            String explanation = "Transaction is incomplete or has an invalid requested amount; no payment should be initiated.";
            checks.add(new PolicyCheck("VALID_TRANSACTION", false, explanation));
            reasons.add(explanation);
            return decision(PolicyDecisionStatus.BLOCKED, 30, reasons, checks);
        }

        BigDecimal amount = context.requestedAmount();
        BigDecimal userLimit = context.user().getSpendingLimit();
        if (amount.compareTo(userLimit) > 0) {
            String explanation = "Requested amount " + money(amount) + " exceeds the user's spending limit of " + money(userLimit) + "; no payment should be initiated.";
            checks.add(new PolicyCheck("USER_SPENDING_LIMIT", false, explanation));
            reasons.add(explanation);
            return decision(PolicyDecisionStatus.BLOCKED, 20, reasons, checks);
        }
        checks.add(new PolicyCheck("USER_SPENDING_LIMIT", true, "Requested amount " + money(amount) + " is within the user's spending limit of " + money(userLimit) + "."));

        if (context.intent().maxAmount() == null || context.intent().maxAmount().signum() <= 0 || amount.compareTo(context.intent().maxAmount()) > 0) {
            String allowed = context.intent().maxAmount() == null ? "an invalid intent maximum" : money(context.intent().maxAmount());
            String explanation = "Requested amount " + money(amount) + " exceeds the structured intent maximum of " + allowed + "; no payment should be initiated.";
            checks.add(new PolicyCheck("INTENT_MAX_AMOUNT", false, explanation));
            reasons.add(explanation);
            return decision(PolicyDecisionStatus.BLOCKED, 30, reasons, checks);
        }
        checks.add(new PolicyCheck("INTENT_MAX_AMOUNT", true, "Requested amount " + money(amount) + " is within the structured intent maximum of " + money(context.intent().maxAmount()) + "."));

        List<PurchasePolicy> matchingPolicies = context.activePolicies().stream()
                .filter(policy -> policy.getCategory().equalsIgnoreCase(context.intent().category()))
                .toList();
        if (context.intent().requiresCategoryMatch() && matchingPolicies.isEmpty()) {
            String explanation = "Transaction category '" + context.intent().category() + " does not match an active purchase policy; no payment should be initiated.";
            checks.add(new PolicyCheck("CATEGORY_MATCH", false, explanation));
            reasons.add(explanation);
            return decision(PolicyDecisionStatus.BLOCKED, 0, reasons, checks);
        }
        if (matchingPolicies.isEmpty()) {
            String explanation = "No applicable active purchase policy is available for this transaction; no payment should be initiated.";
            checks.add(new PolicyCheck("APPLICABLE_POLICY", false, explanation));
            reasons.add(explanation);
            return decision(PolicyDecisionStatus.BLOCKED, 30, reasons, checks);
        }
        checks.add(new PolicyCheck("CATEGORY_MATCH", true, "Transaction category matches an active purchase policy."));

        PurchasePolicy strictestPolicy = matchingPolicies.stream().min(Comparator.comparing(PurchasePolicy::getMaxAmount)).orElseThrow();
        if (amount.compareTo(strictestPolicy.getMaxAmount()) > 0) {
            String explanation = "Requested amount " + money(amount) + " exceeds the applicable policy maximum of " + money(strictestPolicy.getMaxAmount()) + "; no payment should be initiated.";
            checks.add(new PolicyCheck("AMOUNT_LIMIT", false, explanation));
            reasons.add(explanation);
            return decision(PolicyDecisionStatus.BLOCKED, 20, reasons, checks);
        }
        checks.add(new PolicyCheck("AMOUNT_LIMIT", true, "Requested amount " + money(amount) + " is within the applicable policy maximum of " + money(strictestPolicy.getMaxAmount()) + "."));

        BigDecimal projectedHourlySpend = context.hourlySpending().add(amount);
        if (projectedHourlySpend.compareTo(context.user().getHourlySpendingLimit()) > 0) {
            BigDecimal overage = projectedHourlySpend.subtract(context.user().getHourlySpendingLimit());
            String explanation = "Hourly spending limit would be exceeded by " + money(overage) + "; no payment should be initiated.";
            checks.add(new PolicyCheck("HOURLY_SPENDING_LIMIT", false, explanation));
            reasons.add(explanation);
            return decision(PolicyDecisionStatus.BLOCKED, 20, reasons, checks);
        }
        checks.add(new PolicyCheck("HOURLY_SPENDING_LIMIT", true, "Projected hourly spending " + money(projectedHourlySpend) + " is within the hourly limit of " + money(context.user().getHourlySpendingLimit()) + "."));

        int riskScore = riskScore(amount, userLimit, projectedHourlySpend, context.user().getHourlySpendingLimit(), context.duplicateTransaction());
        if (context.duplicateTransaction()) {
            String explanation = "An equivalent merchant, category, and amount transaction was found in the recent duplicate-detection window; manual review is required.";
            checks.add(new PolicyCheck("DUPLICATE_TRANSACTION", false, explanation));
            reasons.add(explanation);
            return decision(PolicyDecisionStatus.MANUAL_REVIEW, riskScore, reasons, checks);
        }
        checks.add(new PolicyCheck("DUPLICATE_TRANSACTION", true, "No equivalent recent transaction was found."));

        if (matchingPolicies.stream().anyMatch(PurchasePolicy::isRequiresApproval)) {
            String explanation = "The applicable purchase policy requires explicit approval; manual review is required.";
            checks.add(new PolicyCheck("REQUIRES_APPROVAL", false, explanation));
            reasons.add(explanation);
            return decision(PolicyDecisionStatus.MANUAL_REVIEW, riskScore, reasons, checks);
        }
        checks.add(new PolicyCheck("REQUIRES_APPROVAL", true, "The applicable purchase policy does not require explicit approval."));
        reasons.add("Transaction is within the user's spending and policy limits.");
        return decision(PolicyDecisionStatus.APPROVED, riskScore, reasons, checks);
    }

    private boolean invalid(PolicyEvaluationContext c) {
        return c == null || c.intent() == null || c.user() == null || c.activePolicies() == null || c.hourlySpending() == null
                || blank(c.merchant()) || blank(c.intent().category()) || c.requestedAmount() == null || c.requestedAmount().signum() <= 0
                || c.intent().maxAmount() == null || c.intent().maxAmount().signum() <= 0 || c.user().getSpendingLimit() == null || c.user().getHourlySpendingLimit() == null;
    }
    private int riskScore(BigDecimal amount, BigDecimal userLimit, BigDecimal projectedHourly, BigDecimal hourlyLimit, boolean duplicate) {
        int score = 0;
        if (amount.compareTo(userLimit.multiply(EIGHTY_PERCENT)) >= 0) score += 20;
        if (projectedHourly.compareTo(hourlyLimit.multiply(EIGHTY_PERCENT)) >= 0) score += 20;
        if (duplicate) score += 40;
        return Math.min(score, 100);
    }
    private PolicyDecision decision(PolicyDecisionStatus status, int risk, List<String> reasons, List<PolicyCheck> checks) { return new PolicyDecision(status, risk, List.copyOf(reasons), List.copyOf(checks)); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String money(BigDecimal amount) { return "₹" + amount.setScale(2); }
}
