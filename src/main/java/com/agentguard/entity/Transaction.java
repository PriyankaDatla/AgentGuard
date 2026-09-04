package com.agentguard.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "transactions")
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false, length = 200) private String merchant;
    @Column(nullable = false, length = 100) private String category;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal requestedAmount;
    @Column(precision = 19, scale = 2) private BigDecimal actualAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private TransactionStatus status;
    @Column(nullable = false) private Integer riskScore;
    @Column(length = 1000) private String decisionReason;
    @Column(length = 100) private String razorpayOrderId;
    @Column(length = 100) private String razorpayPaymentId;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public Long getId(){return id;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;} public String getMerchant(){return merchant;} public void setMerchant(String v){merchant=v;} public String getCategory(){return category;} public void setCategory(String v){category=v;} public BigDecimal getRequestedAmount(){return requestedAmount;} public void setRequestedAmount(BigDecimal v){requestedAmount=v;} public BigDecimal getActualAmount(){return actualAmount;} public void setActualAmount(BigDecimal v){actualAmount=v;} public TransactionStatus getStatus(){return status;} public void setStatus(TransactionStatus v){status=v;} public Integer getRiskScore(){return riskScore;} public void setRiskScore(Integer v){riskScore=v;} public String getDecisionReason(){return decisionReason;} public void setDecisionReason(String v){decisionReason=v;} public String getRazorpayOrderId(){return razorpayOrderId;} public void setRazorpayOrderId(String v){razorpayOrderId=v;} public String getRazorpayPaymentId(){return razorpayPaymentId;} public void setRazorpayPaymentId(String v){razorpayPaymentId=v;} public Instant getCreatedAt(){return createdAt;}
}
