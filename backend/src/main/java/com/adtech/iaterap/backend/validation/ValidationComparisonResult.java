package com.adtech.iaterap.backend.validation;

import java.math.BigDecimal;

public record ValidationComparisonResult(
        ValidationResult result,
        BigDecimal normalizedValue,
        BigDecimal deviation,
        String errorMessage) {
}
