package com.adtech.iaterap.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "fmea_branch_mappings")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FmeaBranchMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String fmeaBranchCode;

    @NotBlank
    private String failureEffect;

    private String failureCause;

    private String detectionMethod;

    private String recommendedAction;

    @Min(1)
    @Max(1000)
    private Integer riskPriorityNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lru_id", nullable = false)
    private Lru lru;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fault_template_id", nullable = false)
    private FaultTemplate faultTemplate;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public FmeaBranchMapping() {
    }

    public FmeaBranchMapping(
            String fmeaBranchCode,
            String failureEffect,
            String failureCause,
            String detectionMethod,
            String recommendedAction,
            Integer riskPriorityNumber,
            Lru lru,
            FaultTemplate faultTemplate
    ) {
        this.fmeaBranchCode = fmeaBranchCode;
        this.failureEffect = failureEffect;
        this.failureCause = failureCause;
        this.detectionMethod = detectionMethod;
        this.recommendedAction = recommendedAction;
        this.riskPriorityNumber = riskPriorityNumber;
        this.lru = lru;
        this.faultTemplate = faultTemplate;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getFmeaBranchCode() {
        return fmeaBranchCode;
    }

    public void setFmeaBranchCode(String fmeaBranchCode) {
        this.fmeaBranchCode = fmeaBranchCode;
    }

    public String getFailureEffect() {
        return failureEffect;
    }

    public void setFailureEffect(String failureEffect) {
        this.failureEffect = failureEffect;
    }

    public String getFailureCause() {
        return failureCause;
    }

    public void setFailureCause(String failureCause) {
        this.failureCause = failureCause;
    }

    public String getDetectionMethod() {
        return detectionMethod;
    }

    public void setDetectionMethod(String detectionMethod) {
        this.detectionMethod = detectionMethod;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public Integer getRiskPriorityNumber() {
        return riskPriorityNumber;
    }

    public void setRiskPriorityNumber(Integer riskPriorityNumber) {
        this.riskPriorityNumber = riskPriorityNumber;
    }

    public Lru getLru() {
        return lru;
    }

    public void setLru(Lru lru) {
        this.lru = lru;
    }

    public FaultTemplate getFaultTemplate() {
        return faultTemplate;
    }

    public void setFaultTemplate(FaultTemplate faultTemplate) {
        this.faultTemplate = faultTemplate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
