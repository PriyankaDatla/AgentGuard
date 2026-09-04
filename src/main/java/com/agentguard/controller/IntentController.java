package com.agentguard.controller;

import com.agentguard.dto.*;
import com.agentguard.intent.IntentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/intent")
public class IntentController {
    private final IntentService intentService;
    public IntentController(IntentService intentService) { this.intentService = intentService; }
    @PostMapping("/parse")
    public ResponseEntity<IntentParseResponse> parse(@Valid @RequestBody IntentParseRequest request) { return ResponseEntity.ok(new IntentParseResponse(intentService.parseIntent(request.request(), request.userId()))); }
}
