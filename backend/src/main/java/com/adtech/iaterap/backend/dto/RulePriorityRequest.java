package com.adtech.iaterap.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RulePriorityRequest(@NotNull @Min(0) Integer priority) {}