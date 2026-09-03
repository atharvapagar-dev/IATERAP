package com.adtech.iaterap.backend.coverage.rule;

import com.adtech.iaterap.backend.entity.RuleConfiguration;
import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.entity.TestStep;
import com.adtech.iaterap.backend.repository.RuleConfigurationRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuleEngineTest {
    @Test
    void structuralFailureStopsLaterRule() {
        RuleConfigurationRepository repository = mock(RuleConfigurationRepository.class);
        AtomicBoolean laterCalled = new AtomicBoolean();
        EngineeringRule structural = rule("STRUCT", RuleCategory.STRUCTURAL, false, false);
        EngineeringRule later = new EngineeringRule() {
            @Override public String getRuleId() { return "LATER"; }
            @Override public RuleCategory getCategory() { return RuleCategory.COVERAGE; }
            @Override public RuleResult evaluate(TestProcedure p, Lru l) {
                laterCalled.set(true); return RuleResult.pass("later");
            }
            @Override public RuleResult evaluate(TestProcedure p, Lru l, List<TestStep> s) {
                laterCalled.set(true); return RuleResult.pass("later");
            }
        };
        when(repository.findByEnabledTrueOrderByPriorityAscRuleIdAsc()).thenReturn(List.of(
                config("LATER", RuleCategory.COVERAGE, 1), config("STRUCT", RuleCategory.STRUCTURAL, 99)));

        var results = new RuleEngine(repository, List.of(later, structural))
                .evaluate(new TestProcedure(), new Lru("LRU"), List.of());

        assertEquals(List.of("STRUCT"), results.keySet().stream().toList());
        assertFalse(results.get("STRUCT").isPassed());
        assertFalse(laterCalled.get());
    }

    @Test
    void disabledRulesAreNotExecutedAndPriorityOrdersRules() {
        RuleConfigurationRepository repository = mock(RuleConfigurationRepository.class);
        StringBuilder order = new StringBuilder();
        EngineeringRule first = rule("FIRST", RuleCategory.COVERAGE, true, order);
        EngineeringRule second = rule("SECOND", RuleCategory.COVERAGE, true, order);
        when(repository.findByEnabledTrueOrderByPriorityAscRuleIdAsc()).thenReturn(List.of(
                config("SECOND", RuleCategory.COVERAGE, 20), config("FIRST", RuleCategory.COVERAGE, 10)));

        var results = new RuleEngine(repository, List.of(first, second))
                .evaluate(new TestProcedure(), new Lru("LRU"), List.of());

        assertEquals("FIRSTSECOND", order.toString());
        assertEquals("FIRST", results.get("FIRST").getRuleId());
        verify(repository).findByEnabledTrueOrderByPriorityAscRuleIdAsc();
    }

    @Test
    void signalRuleRejectsMissingSignal() {
        TestProcedure procedure = new TestProcedure();
        Lru lru = new Lru("LRU");
        procedure.setLru(lru);
        RuleResult result = new TestStepSignalRule().evaluate(procedure, lru, List.of(new TestStep()));
        assertFalse(result.isPassed());
        assertTrue(result.getExplanation().contains("valid Signal"));
        assertEquals(0, result.getDetails().get("stepIndex"));
    }

    private EngineeringRule rule(String id, RuleCategory category, boolean pass, boolean unused) {
        return rule(id, category, pass, new StringBuilder());
    }

    private EngineeringRule rule(String id, RuleCategory category, boolean pass, StringBuilder order) {
        return new EngineeringRule() {
            @Override public String getRuleId() { return id; }
            @Override public RuleCategory getCategory() { return category; }
            @Override public RuleResult evaluate(TestProcedure p, Lru l) {
                order.append(id); return pass ? RuleResult.pass("ok") : RuleResult.fail("invalid");
            }
            @Override public RuleResult evaluate(TestProcedure p, Lru l, List<TestStep> s) {
                order.append(id); return pass ? RuleResult.pass("ok") : RuleResult.fail("invalid");
            }
        };
    }

    private RuleConfiguration config(String id, RuleCategory category, int priority) {
        return new RuleConfiguration(id, category, priority, id, true);
    }
}