package com.agentguard.controller;

import com.agentguard.dto.PaymentOrderResponse;
import com.agentguard.payment.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    public ResponseEntity<PaymentOrderResponse> createOrder(
            @RequestBody CreatePaymentRequest request) {

        return ResponseEntity.ok(
                paymentService.createOrder(request.transactionId())
        );
    }

    public record CreatePaymentRequest(Long transactionId) {}
}