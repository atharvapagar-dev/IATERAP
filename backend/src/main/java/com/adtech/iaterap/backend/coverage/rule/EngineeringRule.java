package com.adtech.iaterap.backend.coverage.rule;

import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.TestProcedure;

/**
 * A single deterministic, side-effect-free engineering rule.
 *
 * Implementations must evaluate only the supplied procedure and LRU context.
 * They must not call or depend on other rule implementations.
 */
@FunctionalInterface
public interface EngineeringRule {

    /** Stable identifier used for configuration, reporting, and audit output. */
    default String getRuleId() {
        return getClass().getSimpleName();
    }

    /** Category used by the Rule Engine when ordering and reporting results. */
    default RuleCategory getCategory() {
        return RuleCategory.STRUCTURAL;
    }

    /** Lower values execute first within the configured rule set. */
    default int getPriority() {
        return 0;
    }

    /** Human-readable description shown in configuration and validation reports. */
    default String getDescription() {
        return getRuleId();
    }

    /** Evaluates this rule without mutating either input. */
    RuleResult evaluate(TestProcedure procedure, Lru lru);
}
