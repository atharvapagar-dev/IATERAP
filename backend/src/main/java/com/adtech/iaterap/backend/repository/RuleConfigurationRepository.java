package com.adtech.iaterap.backend.repository;

import com.adtech.iaterap.backend.coverage.rule.RuleConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RuleConfigurationRepository extends JpaRepository<RuleConfiguration, Long> {
    Optional<RuleConfiguration> findByRuleId(String ruleId);
    List<RuleConfiguration> findByEnabledTrueOrderByPriorityAsc();
}
