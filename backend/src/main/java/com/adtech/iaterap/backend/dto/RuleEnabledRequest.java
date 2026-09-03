package com.adtech.iaterap.backend.dto;

import jakarta.validation.constraints.NotNull;

public record RuleEnabledRequest(@NotNull Boolean enabled) {}