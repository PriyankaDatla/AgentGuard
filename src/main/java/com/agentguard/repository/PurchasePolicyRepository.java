package com.agentguard.repository;
import com.agentguard.entity.PurchasePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PurchasePolicyRepository extends JpaRepository<PurchasePolicy, Long> { List<PurchasePolicy> findByUserIdOrderByCreatedAtDesc(Long userId); }
