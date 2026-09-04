package com.agentguard.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record CreatePolicyRequest(@NotNull @Positive Long userId, @NotBlank @Size(max=100) String category, @NotNull @DecimalMin(value="0.00", inclusive=false) @Digits(integer=17, fraction=2) BigDecimal maxAmount, boolean requiresApproval, boolean active) {}
