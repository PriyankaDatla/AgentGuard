package com.agentguard.policy;

import com.agentguard.entity.PurchasePolicy;
import com.agentguard.entity.User;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class DeterministicPolicyEngineTest {
    private final PolicyEngine engine = new DeterministicPolicyEngine();

    @Test void approvesACompliantTransaction() { assertThat(evaluate("50", "100", "100", "0", false, false).decision()).isEqualTo(PolicyDecisionStatus.APPROVED); }
    @Test void blocksWhenUserLimitIsExceeded() { assertThat(evaluate("101", "100", "200", "0", false, false).decision()).isEqualTo(PolicyDecisionStatus.BLOCKED); }
    @Test void blocksWhenPolicyLimitIsExceeded() { assertThat(evaluate("101", "200", "100", "0", false, false).decision()).isEqualTo(PolicyDecisionStatus.BLOCKED); }
    @Test void blocksWhenActualAmountExceedsIntentMaximum() { PolicyEvaluationContext c = new PolicyEvaluationContext(new PurchaseIntent(1L, "running_shoes", "Nike", new BigDecimal("50"), true), "Nike", new BigDecimal("75"), user("100", "100"), List.of(policy("running_shoes", "100", false)), BigDecimal.ZERO, false); assertThat(engine.evaluate(c).decision()).isEqualTo(PolicyDecisionStatus.BLOCKED); }
    @Test void blocksCategoryMismatch() { assertThat(engine.evaluate(context("20", "100", "100", "0", "gaming_headphones", "running_shoes", false, false)).decision()).isEqualTo(PolicyDecisionStatus.BLOCKED); }
    @Test void blocksWhenHourlyLimitIsExceeded() { assertThat(evaluate("30", "100", "100", "80", false, false).decision()).isEqualTo(PolicyDecisionStatus.BLOCKED); }
    @Test void blocksMissingPolicyInformation() { assertThat(engine.evaluate(new PolicyEvaluationContext(intent("running_shoes"), "Nike", new BigDecimal("20"), user("100", "100"), List.of(), BigDecimal.ZERO, false)).decision()).isEqualTo(PolicyDecisionStatus.BLOCKED); }
    @Test void flagsDuplicatesForManualReview() { PolicyDecision d = evaluate("20", "100", "100", "0", true, false); assertThat(d.decision()).isEqualTo(PolicyDecisionStatus.MANUAL_REVIEW); assertThat(d.riskScore()).isGreaterThanOrEqualTo(40); }
    @Test void flagsApprovalRequirementForManualReview() { assertThat(evaluate("20", "100", "100", "0", false, true).decision()).isEqualTo(PolicyDecisionStatus.MANUAL_REVIEW); }
    @Test void acceptsAnAmountEqualToLimits() { assertThat(evaluate("100", "100", "100", "0", false, false).decision()).isEqualTo(PolicyDecisionStatus.APPROVED); }
    @Test void blocksZeroAmount() { assertThat(evaluate("0", "100", "100", "0", false, false).decision()).isEqualTo(PolicyDecisionStatus.BLOCKED); }
    @Test void blocksNegativeAmount() { assertThat(evaluate("-1", "100", "100", "0", false, false).decision()).isEqualTo(PolicyDecisionStatus.BLOCKED); }
    @Test void blocksVeryLargeAmount() { assertThat(evaluate("99999999999999999.99", "100", "100", "0", false, false).decision()).isEqualTo(PolicyDecisionStatus.BLOCKED); }
    @Test void usesStrictestOfMultipleApplicablePolicies() { PolicyEvaluationContext c = new PolicyEvaluationContext(intent("running_shoes"), "Nike", new BigDecimal("75"), user("100", "100"), List.of(policy("running_shoes", "100", false), policy("running_shoes", "50", false)), BigDecimal.ZERO, false); assertThat(engine.evaluate(c).decision()).isEqualTo(PolicyDecisionStatus.BLOCKED); }
    @Test void duplicateTakesPriorityOverApprovalRequirement() { assertThat(evaluate("20", "100", "100", "0", true, true).policyChecks().getLast().rule()).isEqualTo("DUPLICATE_TRANSACTION"); }
    @Test void calculatesRiskForNearLimits() { assertThat(evaluate("80", "100", "100", "0", false, false).riskScore()).isGreaterThanOrEqualTo(20); }
    @Test void usesBigDecimalComparisonRatherThanFloatingPoint() { assertThat(evaluate("0.10", "0.10", "0.10", "0", false, false).decision()).isEqualTo(PolicyDecisionStatus.APPROVED); }
    @Test void blocksIncompleteMerchant() { PolicyEvaluationContext c = context("20", "100", "100", "0", "", "running_shoes", false, false); assertThat(engine.evaluate(c).decision()).isEqualTo(PolicyDecisionStatus.BLOCKED); }
    @Test void retainsBlockingPriorityOverDuplicate() { assertThat(evaluate("200", "100", "100", "0", true, false).decision()).isEqualTo(PolicyDecisionStatus.BLOCKED); }

    private PolicyDecision evaluate(String amount, String userLimit, String policyLimit, String hourlySpent, boolean duplicate, boolean approvalRequired) { return engine.evaluate(context(amount, userLimit, policyLimit, hourlySpent, "running_shoes", "running_shoes", duplicate, approvalRequired)); }
    private PolicyEvaluationContext context(String amount, String userLimit, String policyLimit, String hourlySpent, String category, String policyCategory, boolean duplicate, boolean approvalRequired) { return new PolicyEvaluationContext(intent(category), "Nike", new BigDecimal(amount), user(userLimit, "100"), List.of(policy(policyCategory, policyLimit, approvalRequired)), new BigDecimal(hourlySpent), duplicate); }
    private PurchaseIntent intent(String category) { return new PurchaseIntent(1L, category, null, new BigDecimal("100"), true); }
    private User user(String spendingLimit, String hourlyLimit) { User u = new User(); u.setName("Ada"); u.setEmail("ada@example.com"); u.setSpendingLimit(new BigDecimal(spendingLimit)); u.setHourlySpendingLimit(new BigDecimal(hourlyLimit)); return u; }
    private PurchasePolicy policy(String category, String amount, boolean approval) { PurchasePolicy p = new PurchasePolicy(); p.setUserId(1L); p.setCategory(category); p.setMaxAmount(new BigDecimal(amount)); p.setActive(true); p.setRequiresApproval(approval); return p; }
}
