package com.agentguard.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "purchase_policies")
public class PurchasePolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false, length = 100) private String category;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal maxAmount;
    @Column(nullable = false) private boolean requiresApproval;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public Long getId(){return id;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;} public String getCategory(){return category;} public void setCategory(String v){category=v;} public BigDecimal getMaxAmount(){return maxAmount;} public void setMaxAmount(BigDecimal v){maxAmount=v;} public boolean isRequiresApproval(){return requiresApproval;} public void setRequiresApproval(boolean v){requiresApproval=v;} public boolean isActive(){return active;} public void setActive(boolean v){active=v;} public Instant getCreatedAt(){return createdAt;}
}
