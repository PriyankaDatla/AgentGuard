package com.agentguard.controller;
import com.agentguard.dto.AuditEventResponse; import com.agentguard.service.AuditService; import org.springframework.web.bind.annotation.*; import java.util.List;
@RestController @RequestMapping("/api/audit")
public class AuditController { private final AuditService service; public AuditController(AuditService service){this.service=service;} @GetMapping("/transaction/{transactionId}") public List<AuditEventResponse> getForTransaction(@PathVariable Long transactionId){return service.getForTransaction(transactionId);} }
