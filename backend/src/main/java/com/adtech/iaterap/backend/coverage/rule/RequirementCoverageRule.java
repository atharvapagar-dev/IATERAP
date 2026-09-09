package com.adtech.iaterap.backend.coverage.rule;

import org.springframework.stereotype.Component;

/** Coverage of steps with an explicit acceptance requirement. */
@Component
public final class RequirementCoverageRule extends AbstractCoverageCategoryRule {
    public RequirementCoverageRule() {
        super("COVERAGE-REQUIREMENT", "Requirement coverage");
    }
    @Override public RuleResult evaluate(com.adtech.iaterap.backend.entity.TestProcedure procedure, com.adtech.iaterap.backend.entity.Lru lru, java.util.List<com.adtech.iaterap.backend.entity.TestStep> steps) {
        return unavailable("No LRU-scoped Requirement catalog or Test Step Requirement reference exists");
    }
}
