package com.adtech.iaterap.backend.coverage.rule;

import org.springframework.stereotype.Component;

/**
 * Coverage of the current FMEA evidence field on a test step. A dedicated
 * FMEA-branch FK is a future schema enhancement; this rule intentionally uses
 * the existing safety-notes contract and does not infer an FMEA mapping from a
 * fault template alone.
 */
@Component
public final class FmeaCoverageRule extends AbstractCoverageCategoryRule {
    public FmeaCoverageRule() {
        super("COVERAGE-FMEA", "FMEA evidence represented by test steps", step -> step != null && hasText(step.getSafetyNotes()));
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
}
