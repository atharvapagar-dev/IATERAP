package com.adtech.iaterap.backend.validation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

/** Deterministic conversion service used by external validation readings. */
public final class UnitConversionService {

    static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;

    private UnitConversionService() {}

    public static UnitValue convert(UnitValue source, EngineeringUnit targetUnit) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(targetUnit, "targetUnit must not be null");

        if (!source.unit().getDimension().equals(targetUnit.getDimension())) {
            throw new IncompatibleUnitException(source.unit(), targetUnit);
        }

        BigDecimal baseValue = source.unit().toBase(source.value());
        return new UnitValue(targetUnit.fromBase(baseValue), targetUnit);
    }

    static final class IncompatibleUnitException extends IllegalArgumentException {
        IncompatibleUnitException(EngineeringUnit source, EngineeringUnit target) {
            super("Cannot convert " + source + " to incompatible unit " + target);
        }
    }
}
