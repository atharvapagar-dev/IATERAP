package com.adtech.iaterap.backend.dto;

import com.adtech.iaterap.backend.entity.RuleConfiguration;
import java.math.BigDecimal;

public record RuleConfigurationResponse(String ruleId, String category, int priority, String description,
                                        boolean enabled, BigDecimal weight, BigDecimal threshold) {
    public static RuleConfigurationResponse from(RuleConfiguration rule) {
        return new RuleConfigurationResponse(rule.getRuleId(), rule.getCategory().name(), rule.getPriority(),
                rule.getDescription(), rule.isEnabled(), rule.getWeight(), rule.getThreshold());
    }
}
