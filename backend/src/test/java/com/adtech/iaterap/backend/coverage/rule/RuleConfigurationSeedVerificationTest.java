package com.adtech.iaterap.backend.coverage.rule;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI-203 / T9-SH: verifies that Flyway creates and seeds the live rule set.
 * This intentionally queries PostgreSQL rather than inspecting migration text.
 */
@SpringBootTest
@ActiveProfiles("synthetic-data")
class RuleConfigurationSeedVerificationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationSeedsInitialStructuralRule() {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT rule_id, category, priority, description, enabled "
                        + "FROM rules_configuration WHERE rule_id = ?",
                "STRUCT-TEST-STEP-SIGNAL");

        assertEquals("STRUCT-TEST-STEP-SIGNAL", row.get("rule_id"));
        assertEquals("STRUCTURAL", row.get("category"));
        assertEquals(10, ((Number) row.get("priority")).intValue());
        assertEquals(
                "Every Test Step must reference a valid Signal for the procedure LRU",
                row.get("description"));
        assertTrue((Boolean) row.get("enabled"));
    }
}
