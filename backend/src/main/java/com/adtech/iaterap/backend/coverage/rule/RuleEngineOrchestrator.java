package com.adtech.iaterap.backend.coverage.rule;

import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.entity.TestStep;
import com.adtech.iaterap.backend.repository.RuleConfigurationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the enabled rule set from persisted configuration and executes it in
 * deterministic priority order. Rule implementations remain code-owned;
 * configuration controls which rules participate and in what order.
 */
@Service
public class RuleEngineOrchestrator {
    private final RuleConfigurationRepository configurationRepository;
    private final Map<String, EngineeringRule> rulesById;

    public RuleEngineOrchestrator(
            RuleConfigurationRepository configurationRepository,
            List<EngineeringRule> rules
    ) {
        this.configurationRepository = configurationRepository;
        Map<String, EngineeringRule> indexed = new LinkedHashMap<>();
        if (rules != null) {
            for (EngineeringRule rule : rules) {
                if (rule == null) {
                    continue;
                }
                EngineeringRule previous = indexed.put(rule.getRuleId(), rule);
                if (previous != null) {
                    throw new IllegalArgumentException(
                            "Duplicate engineering rule id: " + rule.getRuleId());
                }
            }
        }
        this.rulesById = Map.copyOf(indexed);
    }

    /**
     * Evaluates every enabled rule configured in the database. Results retain
     * execution order so callers can report the configured sequence reliably.
     */
    @Transactional(readOnly = true)
    public Map<String, RuleResult> evaluate(
            TestProcedure procedure,
            Lru lru,
            List<TestStep> steps
    ) {
        List<RuleConfiguration> configurations = configurationRepository
                .findByEnabledTrueOrderByPriorityAsc()
                .stream()
                .sorted(Comparator.comparing(RuleConfiguration::getPriority)
                        .thenComparing(RuleConfiguration::getRuleId))
                .toList();

        Map<String, RuleResult> results = new LinkedHashMap<>();
        for (RuleConfiguration configuration : configurations) {
            EngineeringRule rule = rulesById.get(configuration.getRuleId());
            if (rule == null) {
                throw new IllegalStateException(
                        "No implementation registered for enabled rule: "
                                + configuration.getRuleId());
            }
            results.put(configuration.getRuleId(),
                    rule.evaluate(procedure, lru, steps == null ? List.of() : steps));
        }
        return results;
    }
}
