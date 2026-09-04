package com.agentguard.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 255) private String email;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal spendingLimit;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal hourlySpendingLimit;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public Long getId(){return id;} public String getName(){return name;} public void setName(String v){name=v;} public String getEmail(){return email;} public void setEmail(String v){email=v;} public BigDecimal getSpendingLimit(){return spendingLimit;} public void setSpendingLimit(BigDecimal v){spendingLimit=v;} public BigDecimal getHourlySpendingLimit(){return hourlySpendingLimit;} public void setHourlySpendingLimit(BigDecimal v){hourlySpendingLimit=v;} public Instant getCreatedAt(){return createdAt;}
}
