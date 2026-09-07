package com.adtech.iaterap.backend.coverage.rule;

import com.adtech.iaterap.backend.entity.TestStep;
import org.springframework.stereotype.Component;

/** Coverage of steps linked to an Interface. */
@Component
public final class InterfaceCoverageRule extends AbstractCoverageCategoryRule {
    public InterfaceCoverageRule() {
        super("COVERAGE-INTERFACE", "Interface elements referenced by test steps", step -> step != null && step.getInterfaceEntity() != null);
    }
}
