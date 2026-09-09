package com.adtech.iaterap.backend.coverage.rule;

import org.springframework.stereotype.Component;

/** Coverage of steps with an explicit expected functional behaviour. */
@Component
public final class FunctionalCoverageRule extends AbstractCoverageCategoryRule {
    public FunctionalCoverageRule() {
        super("COVERAGE-FUNCTIONAL", "Functional coverage");
    }
    @Override public RuleResult evaluate(com.adtech.iaterap.backend.entity.TestProcedure procedure, com.adtech.iaterap.backend.entity.Lru lru, java.util.List<com.adtech.iaterap.backend.entity.TestStep> steps) {
        return unavailable("No LRU-scoped Functional catalog or Test Step Functional reference exists");
    }
}
