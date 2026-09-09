package com.adtech.iaterap.backend.entity;

import com.adtech.iaterap.backend.coverage.rule.RuleCategory;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "rules_configuration", indexes = {
        @Index(name = "idx_rules_configuration_priority", columnList = "priority")
})
public class RuleConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id", nullable = false, unique = true, length = 100)
    private String ruleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RuleCategory category;

    @Column(nullable = false)
    private Integer priority;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private boolean enabled = true;

    /** Optional percentage contribution used only by composite coverage rules. */
    @Column(precision = 5, scale = 2)
    private BigDecimal weight;

    /** Optional minimum category-coverage percentage used only by coverage rules. */
    @Column(precision = 5, scale = 2)
    private BigDecimal threshold;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public RuleConfiguration() {}

    public RuleConfiguration(String ruleId, RuleCategory category, int priority, String description, boolean enabled) {
        this.ruleId = ruleId;
        this.category = category;
        this.priority = priority;
        this.description = description;
        this.enabled = enabled;
    }

    public Long getId() { return id; }
    public String getRuleId() { return ruleId; }
    public RuleCategory getCategory() { return category; }
    public Integer getPriority() { return priority; }
    public String getDescription() { return description; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public BigDecimal getThreshold() { return threshold; }
    public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
