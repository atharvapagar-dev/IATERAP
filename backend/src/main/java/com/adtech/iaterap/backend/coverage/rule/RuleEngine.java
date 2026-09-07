package com.adtech.iaterap.backend.coverage.rule;

import com.adtech.iaterap.backend.entity.RuleConfiguration;
import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.entity.TestStep;
import com.adtech.iaterap.backend.repository.RuleConfigurationRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

@Service
public class RuleEngine {
    private final RuleConfigurationRepository configurationRepository;
    private final Map<String, EngineeringRule> implementations;

    public RuleEngine(RuleConfigurationRepository configurationRepository, List<EngineeringRule> rules) {
        this.configurationRepository = configurationRepository;
        this.implementations = rules.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                EngineeringRule::getRuleId, rule -> rule));
    }

    public Map<String, RuleResult> evaluate(TestProcedure procedure, Lru lru, List<TestStep> steps) {
        Map<String, RuleResult> results = new LinkedHashMap<>();
        List<RuleConfiguration> configurations = configurationRepository.findByEnabledTrueOrderByPriorityAscRuleIdAsc()
            .stream()
            .sorted(Comparator.comparingInt((RuleConfiguration configuration) ->
                    configuration.getCategory() == RuleCategory.STRUCTURAL ? 0 : 1)
                .thenComparingInt(RuleConfiguration::getPriority)
                .thenComparing(RuleConfiguration::getRuleId))
            .toList();
        for (RuleConfiguration configuration : configurations) {
            EngineeringRule rule = implementations.get(configuration.getRuleId());
            if (rule == null) {
                throw new IllegalStateException("No implementation registered for configured rule " + configuration.getRuleId());
            }
            RuleResult result = rule.evaluate(procedure, lru, steps == null ? List.of() : steps)
                    .withMetadata(configuration.getRuleId(), configuration.getCategory());
            results.put(configuration.getRuleId(), result);
            if (configuration.getCategory() == RuleCategory.STRUCTURAL && !result.isPassed()) {
                break;
            }
        }
        return results;
    }
}