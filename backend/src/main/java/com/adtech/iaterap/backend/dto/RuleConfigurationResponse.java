package com.adtech.iaterap.backend.dto;

import com.adtech.iaterap.backend.entity.RuleConfiguration;

public record RuleConfigurationResponse(String ruleId, String category, int priority, String description, boolean enabled) {
    public static RuleConfigurationResponse from(RuleConfiguration rule) {
        return new RuleConfigurationResponse(rule.getRuleId(), rule.getCategory().name(), rule.getPriority(),
                rule.getDescription(), rule.isEnabled());
    }
}