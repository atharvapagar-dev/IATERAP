package com.adtech.iaterap.backend.validation;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidationComparisonServiceTest {

    private final ValidationComparisonService service = new ValidationComparisonService();

    @Test
    void normalizesUnitsBeforeComparing() {
        ValidationReadingInput reading = new ValidationReadingInput(
                1L, 2, new BigDecimal("2500"), EngineeringUnit.MILLIVOLT, null);

        ValidationComparisonResult result = service.compare(
                "2..3", EngineeringUnit.VOLT, null, reading);

        assertEquals(ValidationResult.MATCH, result.result());
        assertEquals(0, result.normalizedValue().compareTo(new BigDecimal("2.5")));
    }

    @Test
    void distinguishesBorderlineOutlierFromSevereMismatch() {
        ValidationReadingInput borderline = new ValidationReadingInput(
                1L, 2, new BigDecimal("3.05"), EngineeringUnit.VOLT, null);
        ValidationReadingInput severe = new ValidationReadingInput(
                1L, 3, new BigDecimal("4"), EngineeringUnit.VOLT, null);

        assertEquals(ValidationResult.OUTLIER,
                service.compare("2..3", EngineeringUnit.VOLT, null, borderline).result());
        assertEquals(ValidationResult.MISMATCH,
                service.compare("2..3", EngineeringUnit.VOLT, null, severe).result());
    }

    @Test
    void classifiesMissingAndQualitativeReadings() {
        ValidationReadingInput missing = new ValidationReadingInput(1L, 2, null, null, null);
        ValidationReadingInput observed = new ValidationReadingInput(
                1L, 3, null, null, "Nominal");

        assertEquals(ValidationResult.MISSING,
                service.compare("2..3", EngineeringUnit.VOLT, null, missing).result());
        assertEquals(ValidationResult.MATCH,
                service.compare(null, null, "Nominal", observed).result());
    }
}
