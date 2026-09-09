package com.adtech.iaterap.backend.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationFixtureParserTest {

    private final ValidationFixtureParser parser = new ValidationFixtureParser(new ObjectMapper());

    @Test
    void parsesCsvReadingsAndUnits() {
        byte[] fixture = ("testStepId,observedValue,observedUnit,observedBehaviour\n"
                + "101,2500,millivolt,Nominal\n").getBytes(StandardCharsets.UTF_8);

        List<ValidationReadingInput> readings = parser.parse(fixture, "csv");

        assertEquals(1, readings.size());
        assertEquals(101L, readings.get(0).testStepId());
        assertEquals(new BigDecimal("2500"), readings.get(0).observedValue());
        assertEquals(EngineeringUnit.MILLIVOLT, readings.get(0).observedUnit());
    }

    @Test
    void parsesJsonReadings() {
        byte[] fixture = "[{\"testStepId\":101,\"observedValue\":2.5,\"observedUnit\":\"VOLT\"}]"
                .getBytes(StandardCharsets.UTF_8);

        ValidationReadingInput reading = parser.parse(fixture, "JSON").get(0);

        assertEquals(0, new BigDecimal("2.5").compareTo(reading.observedValue()));
        assertEquals(EngineeringUnit.VOLT, reading.observedUnit());
    }

    @Test
    void rejectsDuplicateStepIds() {
        byte[] fixture = ("testStepId,observedValue\n101,1\n101,2\n")
                .getBytes(StandardCharsets.UTF_8);

        assertThrows(ValidationFixtureException.class, () -> parser.parse(fixture, "CSV"));
    }

    @Test
    void rejectsUnknownUnitsAndMissingStepId() {
        byte[] fixture = ("testStepId,observedUnit\n101,knots\n")
                .getBytes(StandardCharsets.UTF_8);

        assertThrows(ValidationFixtureException.class, () -> parser.parse(fixture, "CSV"));
        assertThrows(ValidationFixtureException.class,
                () -> parser.parse("observedValue\n1\n".getBytes(StandardCharsets.UTF_8), "CSV"));
    }

    @Test
    void computesStableSha256() {
        String checksum = parser.sha256("fixture".getBytes(StandardCharsets.UTF_8));

        assertEquals("f16d05ec6b29248d2c61adb1e9263f78e4f7bace1b955014a2d17872cfe4064d",
                checksum);
    }
}
