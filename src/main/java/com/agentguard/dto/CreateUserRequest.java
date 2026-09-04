package com.agentguard.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record CreateUserRequest(@NotBlank @Size(max=120) String name, @NotBlank @Email @Size(max=255) String email, @NotNull @DecimalMin(value="0.00", inclusive=false) @Digits(integer=17, fraction=2) BigDecimal spendingLimit, @NotNull @DecimalMin(value="0.00", inclusive=false) @Digits(integer=17, fraction=2) BigDecimal hourlySpendingLimit) {}
