package com.adtech.iaterap.backend.coverage.rule;

import org.springframework.stereotype.Component;

/** Coverage of steps with an explicit acceptance requirement. */
@Component
public final class RequirementCoverageRule extends AbstractCoverageCategoryRule {
    public RequirementCoverageRule() {
        super("COVERAGE-REQUIREMENT", "Requirement elements represented by test steps", step -> step != null && hasText(step.getAcceptanceCriteria()));
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
}
