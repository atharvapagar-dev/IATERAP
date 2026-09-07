package com.adtech.iaterap.backend.coverage.rule;

import org.springframework.stereotype.Component;

/** Coverage of steps with an explicit expected functional behaviour. */
@Component
public final class FunctionalCoverageRule extends AbstractCoverageCategoryRule {
    public FunctionalCoverageRule() {
        super("COVERAGE-FUNCTIONAL", "Functional elements represented by test steps", step -> step != null && hasText(step.getExpectedBehaviour()));
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
}
