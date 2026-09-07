package com.adtech.iaterap.backend.coverage.rule;

import org.springframework.stereotype.Component;

/** Coverage of steps linked to a Fault Template. */
@Component
public final class FaultCoverageRule extends AbstractCoverageCategoryRule {
    public FaultCoverageRule() {
        super("COVERAGE-FAULT", "Fault elements referenced by test steps", step -> step != null && step.getFaultCategory() != null);
    }
}
