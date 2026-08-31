package com.adtech.iaterap.backend.coverage.rule;

import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.TestProcedure;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EngineeringRuleTest {

    @Test
    void exposesRuleMetadataAndStructuredResult() {
        EngineeringRule rule = new EngineeringRule() {
            public String getRuleId() { return "STRUCT-001"; }
            public RuleCategory getCategory() { return RuleCategory.STRUCTURAL; }
            public int getPriority() { return 10; }
            public String getDescription() { return "Procedure contains at least one step"; }
            public RuleResult evaluate(TestProcedure procedure, Lru lru) {
                return RuleResult.fail("Procedure has no steps", Map.of("procedureId", 1L));
            }
        };

        RuleResult result = rule.evaluate(new TestProcedure(), new Lru("LRU-1"));

        assertEquals("STRUCT-001", rule.getRuleId());
        assertEquals(RuleCategory.STRUCTURAL, rule.getCategory());
        assertEquals(10, rule.getPriority());
        assertFalse(result.isPassed());
        assertEquals("Procedure has no steps", result.getExplanation());
        assertTrue(result.getDetails().containsKey("procedureId"));
        assertThrows(UnsupportedOperationException.class,
                () -> result.getDetails().put("another", "value"));
    }
}
