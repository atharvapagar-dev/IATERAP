package com.adtech.iaterap.backend.validation;

import java.math.BigDecimal;

/** Inclusive numeric range written as {@code lower..upper} or a single value. */
public record ExpectedRange(BigDecimal lower, BigDecimal upper) {

    public ExpectedRange {
        if (lower == null || upper == null || lower.compareTo(upper) > 0) {
            throw new IllegalArgumentException("Expected range bounds are invalid");
        }
    }

    public static ExpectedRange parse(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationFixtureException("Numeric validation requires an expected range");
        }
        String[] bounds = value.trim().split("\\.\\.", -1);
        try {
            BigDecimal lower = new BigDecimal(bounds[0].trim());
            BigDecimal upper = bounds.length == 1
                    ? lower : new BigDecimal(bounds[1].trim());
            if (bounds.length > 2) {
                throw new NumberFormatException();
            }
            return new ExpectedRange(lower, upper);
        } catch (NumberFormatException ex) {
            throw new ValidationFixtureException(
                    "Expected range must use the numeric lower..upper format: " + value);
        }
    }

    public boolean contains(BigDecimal value) {
        return value.compareTo(lower) >= 0 && value.compareTo(upper) <= 0;
    }

    public BigDecimal distanceOutside(BigDecimal value) {
        if (value.compareTo(lower) < 0) {
            return lower.subtract(value);
        }
        if (value.compareTo(upper) > 0) {
            return value.subtract(upper);
        }
        return BigDecimal.ZERO;
    }
}
