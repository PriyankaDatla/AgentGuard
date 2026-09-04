package com.agentguard.controller;

import com.agentguard.dto.*;
import com.agentguard.service.AgentEvaluationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AgentEvaluationService service;
    public AgentController(AgentEvaluationService service) { this.service = service; }
    @PostMapping("/evaluate")
    public ResponseEntity<AgentEvaluateResponse> evaluate(@Valid @RequestBody AgentEvaluateRequest request) { return ResponseEntity.ok(service.evaluate(request)); }
}
