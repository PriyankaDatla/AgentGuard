package com.agentguard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record IntentParseRequest(@NotNull @Positive Long userId, @NotBlank @Size(max = 2_000) String request) {}
