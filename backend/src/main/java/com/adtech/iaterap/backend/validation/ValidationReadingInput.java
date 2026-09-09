package com.adtech.iaterap.backend.validation;

import java.math.BigDecimal;

/** Parsed, unpersisted row from an external validation fixture. */
public record ValidationReadingInput(
        Long testStepId,
        Integer sourceRowNumber,
        BigDecimal observedValue,
        EngineeringUnit observedUnit,
        String observedBehaviour) {
}
