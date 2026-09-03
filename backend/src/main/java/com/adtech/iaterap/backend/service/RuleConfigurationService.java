package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.RuleConfigurationResponse;
import com.adtech.iaterap.backend.entity.RuleConfiguration;
import com.adtech.iaterap.backend.repository.RuleConfigurationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RuleConfigurationService {
    private final RuleConfigurationRepository repository;

    public RuleConfigurationService(RuleConfigurationRepository repository) { this.repository = repository; }

    public List<RuleConfigurationResponse> findAll() {
        return repository.findAll().stream().map(RuleConfigurationResponse::from).toList();
    }

    public RuleConfigurationResponse setEnabled(String ruleId, boolean enabled) {
        RuleConfiguration rule = find(ruleId);
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

    private RuleConfiguration find(String ruleId) {
        if (ruleId == null || ruleId.isBlank()) throw new IllegalArgumentException("Rule id is required.");
        return repository.findByRuleId(ruleId).orElseThrow(() ->
                new java.util.NoSuchElementException("Rule configuration not found: " + ruleId));
    }
}