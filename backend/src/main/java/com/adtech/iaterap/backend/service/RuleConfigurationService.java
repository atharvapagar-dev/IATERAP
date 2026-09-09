package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.RuleConfigurationResponse;
import com.adtech.iaterap.backend.entity.RuleConfiguration;
import com.adtech.iaterap.backend.repository.RuleConfigurationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import java.util.Set;
import com.adtech.iaterap.backend.coverage.rule.RuleCategory;
import com.adtech.iaterap.backend.dto.CoverageScoringConfigurationRequest;
import com.adtech.iaterap.backend.coverage.CoverageScoringConfigurationService;

@Service
public class RuleConfigurationService {
    private final RuleConfigurationRepository repository;

    public RuleConfigurationService(RuleConfigurationRepository repository) { this.repository = repository; }

    public List<RuleConfigurationResponse> findAll() {
        return repository.findAll().stream().map(RuleConfigurationResponse::from).toList();
    }

    public RuleConfigurationResponse setEnabled(String ruleId, boolean enabled) {
        RuleConfiguration rule = find(ruleId);
        if (rule.getCategory() == RuleCategory.COVERAGE) {
            List<RuleConfiguration> enabledCoverageRules = repository.findAll().stream()
                    .filter(candidate -> candidate.getCategory() == RuleCategory.COVERAGE)
                    .filter(candidate -> !candidate.getRuleId().equals(rule.getRuleId()) ? candidate.isEnabled() : enabled)
                    .toList();
            CoverageScoringConfigurationService.validateEnabledCoverageRules(enabledCoverageRules);
        }
        rule.setEnabled(enabled);
        rule.setUpdatedAt(LocalDateTime.now());
        return RuleConfigurationResponse.from(repository.save(rule));
    }

    public RuleConfigurationResponse setPriority(String ruleId, int priority) {
        if (priority < 0) throw new IllegalArgumentException("Priority must be zero or greater.");
        RuleConfiguration rule = find(ruleId);
        rule.setPriority(priority);
        rule.setUpdatedAt(LocalDateTime.now());
        return RuleConfigurationResponse.from(repository.save(rule));
    }

    public List<RuleConfigurationResponse> getCoverageScoringConfiguration() {
        return repository.findAll().stream().filter(rule -> rule.getCategory() == RuleCategory.COVERAGE)
                .sorted(java.util.Comparator.comparing(RuleConfiguration::getRuleId))
                .map(RuleConfigurationResponse::from).toList();
    }

    @org.springframework.transaction.annotation.Transactional
    public List<RuleConfigurationResponse> updateCoverageScoringConfiguration(CoverageScoringConfigurationRequest request) {
        Set<String> expected = Set.of("COVERAGE-INTERFACE", "COVERAGE-SIGNAL", "COVERAGE-REQUIREMENT", "COVERAGE-FUNCTIONAL", "COVERAGE-FAULT", "COVERAGE-FMEA");
        if (request.rules().size() != expected.size() || !request.rules().stream().map(CoverageScoringConfigurationRequest.Item::ruleId).collect(java.util.stream.Collectors.toSet()).equals(expected)) {
            throw new IllegalArgumentException("Provide one scoring configuration for each of the six coverage categories.");
        }
        BigDecimal total = request.rules().stream().filter(CoverageScoringConfigurationRequest.Item::enabled).map(CoverageScoringConfigurationRequest.Item::weight).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.valueOf(100)) != 0) throw new IllegalArgumentException("Active coverage weights must total exactly 100.00.");
        List<RuleConfiguration> updated = request.rules().stream().map(item -> {
            if (item.weight().scale() > 2 || item.threshold().scale() > 2 || item.weight().signum() < 0 || item.weight().compareTo(BigDecimal.valueOf(100)) > 0 || item.threshold().signum() < 0 || item.threshold().compareTo(BigDecimal.valueOf(100)) > 0) throw new IllegalArgumentException("Coverage weights and thresholds must be percentages from 0.00 to 100.00 with at most two decimal places.");
            RuleConfiguration rule = find(item.ruleId());
            if (rule.getCategory() != RuleCategory.COVERAGE) throw new IllegalArgumentException("Only coverage rules may have scoring configuration.");
            rule.setWeight(item.weight()); rule.setThreshold(item.threshold()); rule.setEnabled(item.enabled()); rule.setUpdatedAt(LocalDateTime.now());
            return rule;
        }).toList();
        return repository.saveAll(updated).stream().map(RuleConfigurationResponse::from).toList();
    }

    private RuleConfiguration find(String ruleId) {
        if (ruleId == null || ruleId.isBlank()) throw new IllegalArgumentException("Rule id is required.");
        return repository.findByRuleId(ruleId).orElseThrow(() ->
                new java.util.NoSuchElementException("Rule configuration not found: " + ruleId));
    }
}
