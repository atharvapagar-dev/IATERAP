package com.adtech.iaterap.backend.coverage.rule;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable, auditable output from one EngineeringRule evaluation. */
public final class RuleResult {

    private final boolean passed;
    private final String explanation;
    private final Map<String, Object> details;

    private RuleResult(boolean passed, String explanation, Map<String, Object> details) {
        this.passed = passed;
        this.explanation = explanation == null ? "" : explanation;
        this.details = details == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    public static RuleResult pass(String explanation) {
        return new RuleResult(true, explanation, Map.of());
    }

    public static RuleResult pass(String explanation, Map<String, Object> details) {
        return new RuleResult(true, explanation, details);
    }

    public static RuleResult fail(String explanation) {
        return new RuleResult(false, explanation, Map.of());
    }

    public static RuleResult fail(String explanation, Map<String, Object> details) {
        return new RuleResult(false, explanation, details);
    }

    public boolean isPassed() {
        return passed;
    }

    /** Record-style alias for callers that prefer value accessors. */
    public boolean passed() {
        return passed;
    }

    public String getExplanation() {
        return explanation;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
