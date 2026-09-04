package com.agentguard.service;
import com.agentguard.dto.*; import com.agentguard.entity.PurchasePolicy; import com.agentguard.exception.ResourceNotFoundException; import com.agentguard.repository.*;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.util.List;
@Service @Transactional(readOnly=true)
public class PolicyService {
 private final PurchasePolicyRepository policies; private final UserRepository users; public PolicyService(PurchasePolicyRepository policies,UserRepository users){this.policies=policies;this.users=users;}
 @Transactional public PolicyResponse create(CreatePolicyRequest r){ if(!users.existsById(r.userId())) throw new ResourceNotFoundException("User not found: " + r.userId()); PurchasePolicy p=new PurchasePolicy();p.setUserId(r.userId());p.setCategory(r.category().trim());p.setMaxAmount(r.maxAmount());p.setRequiresApproval(r.requiresApproval());p.setActive(r.active());return toResponse(policies.save(p)); }
 public List<PolicyResponse> getForUser(Long userId){ if(!users.existsById(userId)) throw new ResourceNotFoundException("User not found: " + userId); return policies.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList(); }
 private PolicyResponse toResponse(PurchasePolicy p){return new PolicyResponse(p.getId(),p.getUserId(),p.getCategory(),p.getMaxAmount(),p.isRequiresApproval(),p.isActive(),p.getCreatedAt());}
}
