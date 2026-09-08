package com.adtech.iaterap.backend.validation;

import java.math.BigDecimal;

/**
 * Supported engineering units for externally supplied validation readings.
 *
 * <p>Each unit is expressed as {@code base = value * multiplier + offset}.
 * Keeping the conversion metadata in one registry prevents ad-hoc conversions
 * from being scattered through import and validation code.
 */
public enum EngineeringUnit {
    MILLIVOLT("VOLTAGE", new BigDecimal("0.001"), BigDecimal.ZERO),
    VOLT("VOLTAGE", BigDecimal.ONE, BigDecimal.ZERO),
    MILLIAMPERE("CURRENT", new BigDecimal("0.001"), BigDecimal.ZERO),
    AMPERE("CURRENT", BigDecimal.ONE, BigDecimal.ZERO),
    HERTZ("FREQUENCY", BigDecimal.ONE, BigDecimal.ZERO),
    KILOHERTZ("FREQUENCY", new BigDecimal("1000"), BigDecimal.ZERO),
    CELSIUS("TEMPERATURE", BigDecimal.ONE, BigDecimal.ZERO),
    FAHRENHEIT("TEMPERATURE", new BigDecimal("0.5555555555555556"),
            new BigDecimal("-17.77777777777778"));

    private final String dimension;
    private final BigDecimal multiplier;
    private final BigDecimal offset;

    EngineeringUnit(String dimension, BigDecimal multiplier, BigDecimal offset) {
        this.dimension = dimension;
        this.multiplier = multiplier;
        this.offset = offset;
    }

    public String getDimension() {
        return dimension;
    }

    BigDecimal toBase(BigDecimal value) {
        return value.multiply(multiplier).add(offset);
    }

    BigDecimal fromBase(BigDecimal value) {
        return value.subtract(offset).divide(multiplier, UnitConversionService.MATH_CONTEXT);
    }
}
