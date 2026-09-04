package com.agentguard.service;

import com.agentguard.dto.*;
import com.agentguard.intent.IntentService;
import com.agentguard.policy.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgentEvaluationServiceTest {
    @Mock IntentService intents; @Mock PolicyEvaluationService policies; @InjectMocks AgentEvaluationService service;
    @BeforeEach void setup() { MockitoAnnotations.openMocks(this); }
    @Test void routesParsedIntentToDeterministicPolicyEvaluation() {
        PurchaseIntent intent = new PurchaseIntent(1L, "running_shoes", "Nike", new BigDecimal("5000"), true);
        TransactionEvaluationResponse decision = new TransactionEvaluationResponse(7L, PolicyDecisionStatus.APPROVED, 10, List.of("Within limits"), List.of());
        when(intents.parseIntent(anyString(), eq(1L))).thenReturn(intent); when(policies.evaluate(any(EvaluateTransactionRequest.class), eq(intent))).thenReturn(decision);
        AgentEvaluateResponse response = service.evaluate(new AgentEvaluateRequest(1L, "Buy Nike running shoes under ₹5,000", "Nike", new BigDecimal("4299")));
        assertThat(response.intent()).isEqualTo(intent); assertThat(response.decision()).isEqualTo(decision); verify(policies).evaluate(argThat(r -> r.category().equals("running_shoes") && r.requestedAmount().compareTo(new BigDecimal("4299")) == 0), eq(intent));
    }
}
