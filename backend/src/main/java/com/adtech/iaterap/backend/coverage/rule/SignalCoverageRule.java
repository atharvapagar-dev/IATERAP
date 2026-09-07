package com.adtech.iaterap.backend.coverage.rule;

import org.springframework.stereotype.Component;

/** Coverage of steps linked to a Signal Library element. */
@Component
public final class SignalCoverageRule extends AbstractCoverageCategoryRule {
    public SignalCoverageRule() {
        super("COVERAGE-SIGNAL", "Signal elements referenced by test steps", step -> step != null && step.getSignalLibrary() != null);
    }
}
