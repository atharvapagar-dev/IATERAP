package com.adtech.iaterap.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Organisation-wide reusable Test Block (AI-179 / T7-A1), extending the
 * AI-153 empty shell with the TestStep content fields a block can hold.
 * Intentionally has no Interface/Connector/Signal/Protocol/Fault
 * references of its own: those are LRU-scoped, so a block cannot carry
 * them across LRUs the way "organisation-wide" reuse requires. The
 * target procedure's own bindings are supplied at insert time instead
 * (see TestBlockService.insertIntoProcedure).
 */
@Entity
@Table(name = "test_blocks")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TestBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "expected_behaviour", length = 1000)
    private String expectedBehaviour;

    @Column(name = "expected_range")
    private String expectedRange;

    @Column(name = "calibration_requirement", length = 500)
    private String calibrationRequirement;

    @Column(name = "safety_notes", length = 1000)
    private String safetyNotes;

    @Column(name = "acceptance_criteria", length = 1000)
    private String acceptanceCriteria;

    @Column(length = 1000)
    private String preconditions;

    @Column(length = 1000)
    private String dependencies;

    @Column(name = "post_conditions", length = 1000)
    private String postConditions;

    @Column(name = "pass_fail_criteria", length = 1000)
    private String passFailCriteria;

    @Column(name = "expected_observation", length = 1000)
    private String expectedObservation;

    @Column(name = "engineer_remarks", length = 1000)
    private String engineerRemarks;

    /** Bumped every time this block is inserted into a procedure. */
    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public TestBlock() {}

    public TestBlock(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getExpectedBehaviour() { return expectedBehaviour; }
    public void setExpectedBehaviour(String v) { this.expectedBehaviour = v; }
    public String getExpectedRange() { return expectedRange; }
    public void setExpectedRange(String v) { this.expectedRange = v; }
    public String getCalibrationRequirement() { return calibrationRequirement; }
    public void setCalibrationRequirement(String v) { this.calibrationRequirement = v; }
    public String getSafetyNotes() { return safetyNotes; }
    public void setSafetyNotes(String v) { this.safetyNotes = v; }
    public String getAcceptanceCriteria() { return acceptanceCriteria; }
    public void setAcceptanceCriteria(String v) { this.acceptanceCriteria = v; }
    public String getPreconditions() { return preconditions; }
    public void setPreconditions(String v) { this.preconditions = v; }
    public String getDependencies() { return dependencies; }
    public void setDependencies(String v) { this.dependencies = v; }
    public String getPostConditions() { return postConditions; }
    public void setPostConditions(String v) { this.postConditions = v; }
    public String getPassFailCriteria() { return passFailCriteria; }
    public void setPassFailCriteria(String v) { this.passFailCriteria = v; }
    public String getExpectedObservation() { return expectedObservation; }
    public void setExpectedObservation(String v) { this.expectedObservation = v; }
    public String getEngineerRemarks() { return engineerRemarks; }
    public void setEngineerRemarks(String v) { this.engineerRemarks = v; }
    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer v) { this.usageCount = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}