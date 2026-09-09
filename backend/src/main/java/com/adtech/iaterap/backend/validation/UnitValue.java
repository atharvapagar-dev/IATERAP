package com.adtech.iaterap.backend.validation;

import java.math.BigDecimal;
import java.util.Objects;

/** A numeric engineering value together with its declared unit. */
public record UnitValue(BigDecimal value, EngineeringUnit unit) {

    public UnitValue {
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(unit, "unit must not be null");
    }
}
