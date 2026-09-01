package com.adtech.iaterap.backend.coverage.rule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Persisted metadata for an EngineeringRule; execution logic remains in code. */
@Entity
@Table(name = "rules_configuration")
public class RuleConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "rule_id", nullable = false, unique = true, length = 120)
    private String ruleId;

    @NotBlank
    @Column(nullable = false, length = 30)
    private String category;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer priority;

    @NotBlank
    @Column(nullable = false, length = 500)
    private String description;

    @NotNull
    @Column(nullable = false)
    private Boolean enabled = true;

    public RuleConfiguration() {}

    public RuleConfiguration(String ruleId, String category, Integer priority,
                             String description, Boolean enabled) {
        this.ruleId = ruleId;
        this.category = category;
        this.priority = priority;
        this.description = description;
        this.enabled = enabled;
    }

    public Long getId() { return id; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
