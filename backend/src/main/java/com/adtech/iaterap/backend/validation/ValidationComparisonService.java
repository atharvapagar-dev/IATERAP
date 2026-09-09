package com.adtech.iaterap.backend.validation;

import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Service;

/** Compares independent readings with the expected contract of one Test Step. */
@Service
public class ValidationComparisonService {

    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;
    private static final BigDecimal OUTLIER_FRACTION = new BigDecimal("0.10");

    public ValidationComparisonResult compare(
            String expectedRange,
            EngineeringUnit expectedUnit,
            String expectedBehaviour,
            ValidationReadingInput reading) {
        if (reading == null) {
            return new ValidationComparisonResult(
                    ValidationResult.MISSING, null, null, "No reading was supplied");
        }

        if (reading.observedValue() == null) {
            if (reading.observedBehaviour() == null || expectedBehaviour == null) {
                return new ValidationComparisonResult(
                        ValidationResult.MISSING, null, null, "No comparable reading was supplied");
            }
            boolean matches = expectedBehaviour.trim().equalsIgnoreCase(
                    reading.observedBehaviour().trim());
            return new ValidationComparisonResult(
                    matches ? ValidationResult.MATCH : ValidationResult.MISMATCH,
                    null, null, matches ? null : "Observed behaviour differs from expected behaviour");
        }

        if (expectedUnit == null || reading.observedUnit() == null) {
            return new ValidationComparisonResult(
                    ValidationResult.MISMATCH, null, null,
                    "Both expected and observed units are required for numeric comparison");
        }

        try {
            UnitValue normalized = UnitConversionService.convert(
                    new UnitValue(reading.observedValue(), reading.observedUnit()), expectedUnit);
            ExpectedRange range = ExpectedRange.parse(expectedRange);
            if (range.contains(normalized.value())) {
                return new ValidationComparisonResult(
                        ValidationResult.MATCH, normalized.value(), BigDecimal.ZERO, null);
            }

            BigDecimal deviation = range.distanceOutside(normalized.value());
            BigDecimal width = range.upper().subtract(range.lower()).abs();
            BigDecimal threshold = width.signum() == 0
                    ? BigDecimal.ONE : width.multiply(OUTLIER_FRACTION, MATH_CONTEXT);
            ValidationResult result = deviation.compareTo(threshold) <= 0
                    ? ValidationResult.OUTLIER : ValidationResult.MISMATCH;
            return new ValidationComparisonResult(result, normalized.value(), deviation,
                    result == ValidationResult.OUTLIER
                            ? "Reading is just outside the expected range"
                            : "Reading is outside the expected range");
        } catch (ValidationFixtureException ex) {
            return new ValidationComparisonResult(
                    ValidationResult.MISMATCH, null, null, ex.getMessage());
        }
    }
}
