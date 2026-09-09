package com.adtech.iaterap.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record CoverageScoringConfigurationRequest(@NotEmpty List<@Valid Item> rules) {
    public record Item(@NotBlank String ruleId, @NotNull BigDecimal weight, @NotNull BigDecimal threshold, @NotNull Boolean enabled) { }
}
