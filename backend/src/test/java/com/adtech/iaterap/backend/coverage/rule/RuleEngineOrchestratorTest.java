package com.adtech.iaterap.backend.coverage.rule;

import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.RuleConfiguration;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.repository.RuleConfigurationRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.concurrent.atomic.AtomicBoolean;

class RuleEngineOrchestratorTest {
    @Test
    void executesOnlyEnabledRulesInPriorityOrder() {
        RuleConfigurationRepository repository = mock(RuleConfigurationRepository.class);
        EngineeringRule late = rule("RULE-LATE", RuleCategory.COVERAGE, RuleResult.pass("late"));
        EngineeringRule early = rule("RULE-EARLY", RuleCategory.STRUCTURAL, RuleResult.pass("early"));
        when(repository.findByEnabledTrueOrderByPriorityAscRuleIdAsc()).thenReturn(List.of(
                new RuleConfiguration("RULE-LATE", RuleCategory.COVERAGE, 20, "late", true),
                new RuleConfiguration("RULE-EARLY", RuleCategory.STRUCTURAL, 10, "early", true)
        ));

        Map<String, RuleResult> results = new RuleEngine(
                repository, List.of(late, early))
                .evaluate(new TestProcedure(), new Lru("LRU-1"), null);

        assertEquals(List.of("RULE-EARLY", "RULE-LATE"), results.keySet().stream().toList());
    }

    @Test
    void rejectsEnabledConfigurationWithoutImplementation() {
        RuleConfigurationRepository repository = mock(RuleConfigurationRepository.class);
        when(repository.findByEnabledTrueOrderByPriorityAscRuleIdAsc()).thenReturn(List.of(
                new RuleConfiguration("MISSING", RuleCategory.STRUCTURAL, 1, "missing", true)
        ));

        RuleEngine orchestrator = new RuleEngine(repository, List.of());

        assertThrows(IllegalStateException.class,
                () -> orchestrator.evaluate(new TestProcedure(), new Lru("LRU-1"), List.of()));
    }

    @Test
    void stopsBeforeCoverageRulesWhenStructuralRuleFails() {
        RuleConfigurationRepository repository = mock(RuleConfigurationRepository.class);
        AtomicBoolean coverageInvoked = new AtomicBoolean();
        EngineeringRule structural = rule("STRUCT-INVALID", RuleCategory.STRUCTURAL, RuleResult.fail("invalid"));
        EngineeringRule coverage = new EngineeringRule() {
            @Override public String getRuleId() { return "COVERAGE-LATE"; }
            @Override public RuleCategory getCategory() { return RuleCategory.COVERAGE; }
            @Override public RuleResult evaluate(TestProcedure procedure, Lru lru) { return RuleResult.pass("unused"); }
            @Override public RuleResult evaluate(TestProcedure procedure, Lru lru, List<com.adtech.iaterap.backend.entity.TestStep> steps) {
                coverageInvoked.set(true);
                return RuleResult.pass("should not run");
            }
        };
        when(repository.findByEnabledTrueOrderByPriorityAscRuleIdAsc()).thenReturn(List.of(
                new RuleConfiguration("COVERAGE-LATE", RuleCategory.COVERAGE, 1, "coverage", true),
                new RuleConfiguration("STRUCT-INVALID", RuleCategory.STRUCTURAL, 99, "structural", true)
        ));

        Map<String, RuleResult> results = new RuleEngine(repository, List.of(structural, coverage))
                .evaluate(new TestProcedure(), new Lru("LRU-1"), List.of());

        assertEquals(List.of("STRUCT-INVALID"), results.keySet().stream().toList());
        assertTrue(!coverageInvoked.get());
    }

    @Test
    void convertsNullStepInputToEmptyListForRules() {
        RuleConfigurationRepository repository = mock(RuleConfigurationRepository.class);
        AtomicBoolean observedEmptySteps = new AtomicBoolean();
        EngineeringRule rule = new EngineeringRule() {
            @Override public String getRuleId() { return "STRUCT-EMPTY-STEPS"; }
            @Override public RuleCategory getCategory() { return RuleCategory.STRUCTURAL; }
            @Override public RuleResult evaluate(TestProcedure procedure, Lru lru) { return RuleResult.pass("unused"); }
            @Override public RuleResult evaluate(TestProcedure procedure, Lru lru, List<com.adtech.iaterap.backend.entity.TestStep> steps) {
                observedEmptySteps.set(steps != null && steps.isEmpty());
                return RuleResult.pass("ok");
            }
        };
        when(repository.findByEnabledTrueOrderByPriorityAscRuleIdAsc()).thenReturn(List.of(
                new RuleConfiguration("STRUCT-EMPTY-STEPS", RuleCategory.STRUCTURAL, 1, "structural", true)
        ));

        new RuleEngine(repository, List.of(rule)).evaluate(new TestProcedure(), new Lru("LRU-1"), null);

        assertTrue(observedEmptySteps.get());
    }

    private EngineeringRule rule(String id, RuleCategory category, RuleResult result) {
        return new EngineeringRule() {
            @Override public String getRuleId() { return id; }
            @Override public RuleCategory getCategory() { return category; }
            @Override public RuleResult evaluate(TestProcedure procedure, Lru lru) { return result; }
        };
    }
}
