package com.agentguard.controller;
import com.agentguard.dto.*; import com.agentguard.service.PolicyService; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import java.util.List;
@RestController @RequestMapping("/api/policies")
public class PolicyController { private final PolicyService service; public PolicyController(PolicyService service){this.service=service;} @PostMapping public ResponseEntity<PolicyResponse> create(@Valid @RequestBody CreatePolicyRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));} @GetMapping("/user/{userId}") public List<PolicyResponse> getForUser(@PathVariable Long userId){return service.getForUser(userId);} }
