package com.agentguard.controller;
import com.agentguard.dto.*; import com.agentguard.service.UserService; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/users")
public class UserController { private final UserService service; public UserController(UserService service){this.service=service;} @PostMapping public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));} @GetMapping("/{id}") public UserResponse get(@PathVariable Long id){return service.getById(id);} }
