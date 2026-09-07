package com.adtech.iaterap.backend.coverage.rule;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable, auditable output from one EngineeringRule evaluation. */
public final class RuleResult {

    private final boolean passed;
    private final String ruleId;
    private final RuleCategory category;
    private final String explanation;
    private final Map<String, Object> details;

    private RuleResult(boolean passed, String ruleId, RuleCategory category, String explanation, Map<String, Object> details) {
        this.passed = passed;
        this.ruleId = ruleId;
        this.category = category;
        this.explanation = explanation == null ? "" : explanation;
        this.details = details == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    public static RuleResult pass(String explanation) {
        return new RuleResult(true, null, null, explanation, Map.of());
    }

    public static RuleResult pass(String explanation, Map<String, Object> details) {
        return new RuleResult(true, null, null, explanation, details);
    }

    public static RuleResult fail(String explanation) {
        return new RuleResult(false, null, null, explanation, Map.of());
    }

    public static RuleResult fail(String explanation, Map<String, Object> details) {
        return new RuleResult(false, null, null, explanation, details);
    }

    public RuleResult withMetadata(String ruleId, RuleCategory category) {
        return new RuleResult(passed, ruleId, category, explanation, details);
    }

    public boolean isPassed() {
        return passed;
    }

    /** Record-style alias for callers that prefer value accessors. */
    public boolean passed() {
        return passed;
    }

    public String getRuleId() { return ruleId; }
    public RuleCategory getCategory() { return category; }

    public String getExplanation() {
        return explanation;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
