package com.agentguard.controller;

import com.agentguard.intent.IntentService;
import com.agentguard.policy.PurchaseIntent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IntentController.class)
class IntentControllerTest {
    @Autowired MockMvc mvc; @MockBean IntentService intents;
    @Test void parsesIntent() throws Exception { when(intents.parseIntent(anyString(), eq(1L))).thenReturn(new PurchaseIntent(1L, "running_shoes", "Nike", new BigDecimal("5000"), true)); mvc.perform(post("/api/intent/parse").contentType(APPLICATION_JSON).content("{\"userId\":1,\"request\":\"Buy Nike running shoes under ₹5,000\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.intent.category").value("running_shoes")); }
    @Test void rejectsBlankRequest() throws Exception { mvc.perform(post("/api/intent/parse").contentType(APPLICATION_JSON).content("{\"userId\":1,\"request\":\"\"}")).andExpect(status().isBadRequest()); }
}
