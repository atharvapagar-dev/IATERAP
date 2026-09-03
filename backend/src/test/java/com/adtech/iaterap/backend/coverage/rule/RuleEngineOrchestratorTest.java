package com.adtech.iaterap.backend.coverage.rule;

import com.adtech.iaterap.backend.entity.Lru;
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
        EngineeringRule late = rule("RULE-LATE", RuleResult.pass("late"));
        EngineeringRule early = rule("RULE-EARLY", RuleResult.pass("early"));
        when(repository.findByEnabledTrueOrderByPriorityAsc()).thenReturn(List.of(
                new RuleConfiguration("RULE-LATE", "COVERAGE", 20, "late", true),
                new RuleConfiguration("RULE-EARLY", "STRUCTURAL", 10, "early", true)
        ));

        Map<String, RuleResult> results = new RuleEngineOrchestrator(
                repository, List.of(late, early))
                .evaluate(new TestProcedure(), new Lru("LRU-1"), null);

        assertEquals(List.of("RULE-EARLY", "RULE-LATE"), results.keySet().stream().toList());
    }

    @Test
    void rejectsEnabledConfigurationWithoutImplementation() {
        RuleConfigurationRepository repository = mock(RuleConfigurationRepository.class);
        when(repository.findByEnabledTrueOrderByPriorityAsc()).thenReturn(List.of(
                new RuleConfiguration("MISSING", "STRUCTURAL", 1, "missing", true)
        ));

        RuleEngineOrchestrator orchestrator = new RuleEngineOrchestrator(repository, List.of());

        assertThrows(IllegalStateException.class,
                () -> orchestrator.evaluate(new TestProcedure(), new Lru("LRU-1"), List.of()));
    }

    private EngineeringRule rule(String id, RuleResult result) {
        return new EngineeringRule() {
            @Override public String getRuleId() { return id; }
            @Override public RuleResult evaluate(TestProcedure procedure, Lru lru) { return result; }
        };
    }
}
