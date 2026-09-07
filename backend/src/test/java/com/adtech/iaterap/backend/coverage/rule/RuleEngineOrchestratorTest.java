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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    private EngineeringRule rule(String id, RuleCategory category, RuleResult result) {
        return new EngineeringRule() {
            @Override public String getRuleId() { return id; }
            @Override public RuleCategory getCategory() { return category; }
            @Override public RuleResult evaluate(TestProcedure procedure, Lru lru) { return result; }
        };
    }
}
