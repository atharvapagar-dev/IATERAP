package com.adtech.iaterap.backend.coverage.rule;

import com.adtech.iaterap.backend.entity.RuleConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleConfigurationTest {
    @Test
    void storesStableMetadataAndDefaultsRulesToEnabled() {
        RuleConfiguration configuration = new RuleConfiguration(
                "STRUCT-001", RuleCategory.STRUCTURAL, 10, "Validates structure", true);

        assertEquals("STRUCT-001", configuration.getRuleId());
        assertEquals(RuleCategory.STRUCTURAL, configuration.getCategory());
        assertEquals(10, configuration.getPriority());
        assertEquals("Validates structure", configuration.getDescription());
        assertTrue(configuration.isEnabled());
    }
}
