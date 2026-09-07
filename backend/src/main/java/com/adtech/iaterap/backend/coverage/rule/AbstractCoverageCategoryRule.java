package com.adtech.iaterap.backend.coverage.rule;

import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.entity.TestStep;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Shared deterministic coverage calculation for one procedure-step category. */
abstract class AbstractCoverageCategoryRule implements EngineeringRule {
    private final String ruleId;
    private final String description;
    private final Predicate<TestStep> reference;

    protected AbstractCoverageCategoryRule(
            String ruleId,
            String description,
            Predicate<TestStep> reference
    ) {
        this.ruleId = ruleId;
        this.description = description;
        this.reference = reference;
    }

    @Override public final String getRuleId() { return ruleId; }
    @Override public final RuleCategory getCategory() { return RuleCategory.COVERAGE; }
    @Override public final String getDescription() { return description; }

    @Override
    public final RuleResult evaluate(TestProcedure procedure, Lru lru) {
        return evaluate(procedure, lru, List.of());
    }

    @Override
    public final RuleResult evaluate(TestProcedure procedure, Lru lru, List<TestStep> steps) {
        List<TestStep> safeSteps = steps == null ? List.of() : steps;
        long referenced = safeSteps.stream().filter(reference).count();
        int total = safeSteps.size();
        double percentage = total == 0 ? 0.0 : referenced * 100.0 / total;

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("category", ruleId.substring("COVERAGE-".length()));
        details.put("referencedElements", referenced);
        details.put("totalElements", total);
        details.put("percentage", percentage);

        return RuleResult.pass(description + " coverage calculated", details);
    }
}
