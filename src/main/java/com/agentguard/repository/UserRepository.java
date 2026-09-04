package com.agentguard.repository;
import com.agentguard.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRepository extends JpaRepository<User, Long> { boolean existsByEmailIgnoreCase(String email); }
