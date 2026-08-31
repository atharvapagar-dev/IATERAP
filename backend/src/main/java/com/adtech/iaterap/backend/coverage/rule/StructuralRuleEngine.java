package com.adtech.iaterap.backend.coverage.rule;

import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.entity.TestStep;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Executes structural rules first and stops before non-structural rules on failure. */
public final class StructuralRuleEngine {
    private final List<EngineeringRule> rules;

    public StructuralRuleEngine(List<EngineeringRule> rules) {
        this.rules = rules == null ? List.of() : rules.stream()
                .sorted(Comparator.comparingInt((EngineeringRule rule) ->
                                rule.getCategory() == RuleCategory.STRUCTURAL ? 0 : 1)
                        .thenComparingInt(EngineeringRule::getPriority))
                .toList();
    }

    public Map<String, RuleResult> evaluate(TestProcedure procedure, Lru lru, List<TestStep> steps) {
        Map<String, RuleResult> results = new LinkedHashMap<>();
        for (EngineeringRule rule : rules) {
            if (rule.getCategory() != RuleCategory.STRUCTURAL && results.values().stream().anyMatch(r -> !r.isPassed())) {
                break;
            }
            RuleResult result = rule.evaluate(procedure, lru, steps == null ? List.of() : steps);
            results.put(rule.getRuleId(), result);
            if (rule.getCategory() == RuleCategory.STRUCTURAL && !result.isPassed()) {
                break;
            }
        }
        return results;
    }
}
