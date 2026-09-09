package com.adtech.iaterap.backend.coverage.rule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Shared result construction for deterministic, defined-element coverage rules. */
abstract class AbstractCoverageCategoryRule implements EngineeringRule {
    private final String ruleId;
    private final String description;

    protected AbstractCoverageCategoryRule(String ruleId, String description) {
        this.ruleId = ruleId;
        this.description = description;
    }

    @Override public final String getRuleId() { return ruleId; }
    @Override public final RuleCategory getCategory() { return RuleCategory.COVERAGE; }
    @Override public final String getDescription() { return description; }

    @Override
    public RuleResult evaluate(com.adtech.iaterap.backend.entity.TestProcedure procedure,
                               com.adtech.iaterap.backend.entity.Lru lru) {
        return unavailable("Ordered Test Steps are required for coverage evaluation");
    }

    protected final RuleResult measured(Set<Long> definedIds, Set<Long> referencedIds) {
        Set<Long> covered = referencedIds.stream().filter(definedIds::contains)
                .collect(java.util.stream.Collectors.toSet());
        Map<String, Object> details = details(true);
        details.put("referencedElements", covered.size());
        details.put("totalElements", definedIds.size());
        if (definedIds.isEmpty()) {
            details.put("percentage", null);
            details.put("reason", "No defined LRU elements exist for this category");
            return RuleResult.pass(description + " is not applicable because no LRU elements are defined", details);
        }
        details.put("percentage", BigDecimal.valueOf(covered.size()).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(definedIds.size()), 2, RoundingMode.HALF_UP));
        return RuleResult.pass(description + " coverage calculated from distinct referenced LRU elements", details);
    }

    protected final RuleResult unavailable(String reason) {
        Map<String, Object> details = details(false);
        details.put("percentage", null);
        details.put("referencedElements", null);
        details.put("totalElements", null);
        details.put("reason", reason);
        return RuleResult.pass(description + " is unavailable: " + reason, details);
    }

    private Map<String, Object> details(boolean measurable) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("category", ruleId.substring("COVERAGE-".length()));
        details.put("measurable", measurable);
        return details;
    }
}
