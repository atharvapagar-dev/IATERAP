package com.adtech.iaterap.backend.coverage.rule;

import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.entity.TestStep;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoverageCategoryRulesTest {
    @Test
    void exposesAllSixCoverageCategoriesWithStructuredPercentages() {
        List<EngineeringRule> rules = List.of(
                new InterfaceCoverageRule(), new SignalCoverageRule(),
                new RequirementCoverageRule(), new FunctionalCoverageRule(),
                new FaultCoverageRule(), new FmeaCoverageRule());

        assertEquals(6, rules.size());
        assertEquals(List.of(
                "COVERAGE-INTERFACE", "COVERAGE-SIGNAL", "COVERAGE-REQUIREMENT",
                "COVERAGE-FUNCTIONAL", "COVERAGE-FAULT", "COVERAGE-FMEA"),
                rules.stream().map(EngineeringRule::getRuleId).toList());

        TestStep referenced = new TestStep();
        referenced.setAcceptanceCriteria("Requirement");
        referenced.setExpectedBehaviour("Function");
        referenced.setSafetyNotes("FMEA branch evidence");
        TestStep missing = new TestStep();

        RuleResult result = new RequirementCoverageRule().evaluate(
                new TestProcedure(), null, List.of(referenced, missing));

        assertTrue(result.isPassed());
        assertEquals(1L, result.getDetails().get("referencedElements"));
        assertEquals(2, result.getDetails().get("totalElements"));
        assertEquals(50.0, result.getDetails().get("percentage"));
    }
}
