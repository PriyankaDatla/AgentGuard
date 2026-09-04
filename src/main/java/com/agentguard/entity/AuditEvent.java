package com.agentguard.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "audit_events")
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long transactionId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private AuditEventType eventType;
    @Column(nullable = false, length = 1000) private String description;
    @Column(columnDefinition = "TEXT") private String metadata;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public Long getId(){return id;} public Long getTransactionId(){return transactionId;} public void setTransactionId(Long v){transactionId=v;} public AuditEventType getEventType(){return eventType;} public void setEventType(AuditEventType v){eventType=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;} public String getMetadata(){return metadata;} public void setMetadata(String v){metadata=v;} public Instant getCreatedAt(){return createdAt;}
}
