package com.adtech.iaterap.backend.coverage;

import com.adtech.iaterap.backend.coverage.rule.RuleCategory;
import com.adtech.iaterap.backend.entity.RuleConfiguration;
import com.adtech.iaterap.backend.repository.RuleConfigurationRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/** Reads live enabled coverage weights and thresholds from Week 9 rule configuration. */
@Service
public class CoverageScoringConfigurationService {
    private static final Set<String> IDS = Set.of("COVERAGE-INTERFACE", "COVERAGE-SIGNAL", "COVERAGE-REQUIREMENT", "COVERAGE-FUNCTIONAL", "COVERAGE-FAULT", "COVERAGE-FMEA");
    private final RuleConfigurationRepository repository;
    public CoverageScoringConfigurationService(RuleConfigurationRepository repository) { this.repository = repository; }
    public List<ScoringRule> activeCoverageRules() {
        List<RuleConfiguration> rules = repository.findByCategoryAndEnabledTrueOrderByPriorityAscRuleIdAsc(RuleCategory.COVERAGE);
        validateEnabledCoverageRules(rules);
        return rules.stream().map(rule -> new ScoringRule(rule.getRuleId(), rule.getWeight(), rule.getThreshold() == null ? BigDecimal.ZERO : rule.getThreshold())).toList();
    }

    /** Shared invariant for runtime scoring and individual coverage enable/disable changes. */
    public static void validateEnabledCoverageRules(List<RuleConfiguration> rules) {
        if (rules.isEmpty() || !IDS.containsAll(rules.stream().map(RuleConfiguration::getRuleId).collect(java.util.stream.Collectors.toSet()))) {
            throw new IllegalStateException("At least one enabled configured coverage rule is required for composite scoring.");
        }
        BigDecimal total = rules.stream().map(RuleConfiguration::getWeight)
                .peek(weight -> { if (weight == null) throw new IllegalStateException("Enabled coverage rules require a configured weight."); })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.valueOf(100)) != 0) throw new IllegalStateException("Enabled coverage rule weights must total 100.00.");
    }
    public record ScoringRule(String ruleId, BigDecimal weight, BigDecimal threshold) { }
}
