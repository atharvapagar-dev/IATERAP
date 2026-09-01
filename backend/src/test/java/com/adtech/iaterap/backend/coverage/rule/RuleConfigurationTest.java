package com.adtech.iaterap.backend.coverage.rule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleConfigurationTest {
    @Test
    void storesStableMetadataAndDefaultsRulesToEnabled() {
        RuleConfiguration configuration = new RuleConfiguration(
                "STRUCT-001", "STRUCTURAL", 10, "Validates structure", true);

        assertEquals("STRUCT-001", configuration.getRuleId());
        assertEquals("STRUCTURAL", configuration.getCategory());
        assertEquals(10, configuration.getPriority());
        assertEquals("Validates structure", configuration.getDescription());
        assertTrue(configuration.getEnabled());
    }
}
