package com.agentguard.repository;
import com.agentguard.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> { List<AuditEvent> findByTransactionIdOrderByCreatedAtAsc(Long transactionId); }
