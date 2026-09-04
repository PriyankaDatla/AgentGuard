package com.agentguard.service;
import com.agentguard.dto.*; import com.agentguard.entity.User; import com.agentguard.exception.*; import com.agentguard.repository.UserRepository;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(readOnly=true)
public class UserService {
 private final UserRepository users; public UserService(UserRepository users){this.users=users;}
 @Transactional public UserResponse create(CreateUserRequest request) { String email = request.email().trim().toLowerCase(); if(users.existsByEmailIgnoreCase(email)) throw new DuplicateResourceException("A user with this email already exists"); User u=new User(); u.setName(request.name().trim()); u.setEmail(email); u.setSpendingLimit(request.spendingLimit()); u.setHourlySpendingLimit(request.hourlySpendingLimit()); return toResponse(users.save(u)); }
 public UserResponse getById(Long id) { return toResponse(users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found: " + id))); }
 private UserResponse toResponse(User u){ return new UserResponse(u.getId(),u.getName(),u.getEmail(),u.getSpendingLimit(),u.getHourlySpendingLimit(),u.getCreatedAt()); }
}
