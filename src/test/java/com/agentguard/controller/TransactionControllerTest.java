package com.agentguard.controller;

import com.agentguard.dto.TransactionEvaluationResponse;
import com.agentguard.policy.PolicyDecisionStatus;
import com.agentguard.service.PolicyEvaluationService;
import com.agentguard.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {
    @Autowired MockMvc mvc;
    @MockBean TransactionService transactions;
    @MockBean PolicyEvaluationService evaluations;
    @Test void evaluatesTransaction() throws Exception {
        Mockito.when(evaluations.evaluate(any())).thenReturn(new TransactionEvaluationResponse(9L, PolicyDecisionStatus.APPROVED, 0, List.of("Approved"), List.of()));
        mvc.perform(post("/api/transactions/evaluate").contentType(MediaType.APPLICATION_JSON).content("{\"userId\":1,\"merchant\":\"Nike\",\"category\":\"running_shoes\",\"requestedAmount\":4299.00}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.transactionId").value(9)).andExpect(jsonPath("$.decision").value("APPROVED"));
    }
    @Test void rejectsInvalidAmount() throws Exception {
        mvc.perform(post("/api/transactions/evaluate").contentType(MediaType.APPLICATION_JSON).content("{\"userId\":1,\"merchant\":\"Nike\",\"category\":\"running_shoes\",\"requestedAmount\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.validationErrors.requestedAmount").exists());
    }
}
