package com.adtech.iaterap.backend.validation;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnitConversionServiceTest {

    @Test
    void convertsMillivoltsToVolts() {
        UnitValue converted = UnitConversionService.convert(
                new UnitValue(new BigDecimal("2500"), EngineeringUnit.MILLIVOLT),
                EngineeringUnit.VOLT);

        assertEquals(new BigDecimal("2.500"), converted.value());
        assertEquals(EngineeringUnit.VOLT, converted.unit());
    }

    @Test
    void convertsFahrenheitToCelsius() {
        UnitValue converted = UnitConversionService.convert(
                new UnitValue(new BigDecimal("212"), EngineeringUnit.FAHRENHEIT),
                EngineeringUnit.CELSIUS);

        assertTrue(converted.value().subtract(new BigDecimal("100")).abs()
                        .compareTo(new BigDecimal("1E-12")) < 0);
    }

    @Test
    void rejectsCrossDimensionConversion() {
        assertThrows(UnitConversionService.IncompatibleUnitException.class,
                () -> UnitConversionService.convert(
                        new UnitValue(BigDecimal.ONE, EngineeringUnit.VOLT),
                        EngineeringUnit.AMPERE));
    }

    @Test
    void roundTripsTemperatureWithoutChangingTheDimension() {
        UnitValue fahrenheit = new UnitValue(new BigDecimal("72.5"), EngineeringUnit.FAHRENHEIT);

        UnitValue roundTrip = UnitConversionService.convert(
                UnitConversionService.convert(fahrenheit, EngineeringUnit.CELSIUS),
                EngineeringUnit.FAHRENHEIT);

        assertTrue(roundTrip.value().subtract(fahrenheit.value()).abs()
                        .compareTo(new BigDecimal("1E-12")) < 0);
    }
}
