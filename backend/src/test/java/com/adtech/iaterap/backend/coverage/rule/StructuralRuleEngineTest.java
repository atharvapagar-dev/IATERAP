package com.adtech.iaterap.backend.coverage.rule;

import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.entity.TestStep;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class StructuralRuleEngineTest {
    @Test
    void stopsAfterStructuralFailure() {
        AtomicBoolean coverageCalled = new AtomicBoolean();
        EngineeringRule structural = new EngineeringRule() {
            @Override public RuleResult evaluate(TestProcedure p, Lru l) { return RuleResult.fail("invalid structure"); }
            @Override public RuleResult evaluate(TestProcedure p, Lru l, List<TestStep> s) { return RuleResult.fail("invalid structure"); }
            @Override public RuleCategory getCategory() { return RuleCategory.STRUCTURAL; }
            @Override public String getRuleId() { return "STRUCT-1"; }
        };
        EngineeringRule coverage = new EngineeringRule() {
            @Override public RuleResult evaluate(TestProcedure p, Lru l) { coverageCalled.set(true); return RuleResult.pass("ok"); }
            @Override public RuleResult evaluate(TestProcedure p, Lru l, List<TestStep> s) { coverageCalled.set(true); return RuleResult.pass("ok"); }
            @Override public RuleCategory getCategory() { return RuleCategory.COVERAGE; }
            @Override public String getRuleId() { return "COVER-1"; }
        };

        var results = new StructuralRuleEngine(List.of(coverage, structural))
                .evaluate(new TestProcedure(), new Lru("LRU-1"), List.of());

        assertEquals(List.of("STRUCT-1"), results.keySet().stream().toList());
        assertFalse(coverageCalled.get());
    }

    @Test
    void runsNonStructuralRulesWhenAllStructuralRulesPass() {
        AtomicBoolean coverageCalled = new AtomicBoolean();
        EngineeringRule structural = new EngineeringRule() {
            @Override public RuleResult evaluate(TestProcedure p, Lru l) { return RuleResult.pass("valid"); }
            @Override public RuleCategory getCategory() { return RuleCategory.STRUCTURAL; }
            @Override public String getRuleId() { return "STRUCT-1"; }
        };
        EngineeringRule coverage = new EngineeringRule() {
            @Override public RuleResult evaluate(TestProcedure p, Lru l) {
                coverageCalled.set(true);
                return RuleResult.pass("covered");
            }
            @Override public RuleCategory getCategory() { return RuleCategory.COVERAGE; }
            @Override public String getRuleId() { return "COVER-1"; }
        };

        var results = new StructuralRuleEngine(List.of(coverage, structural))
                .evaluate(new TestProcedure(), new Lru("LRU-1"), null);

        assertEquals(List.of("STRUCT-1", "COVER-1"), results.keySet().stream().toList());
        assertTrue(coverageCalled.get());
    }
}
