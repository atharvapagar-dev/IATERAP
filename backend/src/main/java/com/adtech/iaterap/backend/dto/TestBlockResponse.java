package com.adtech.iaterap.backend.dto;

import com.adtech.iaterap.backend.entity.TestBlock;

import java.time.LocalDateTime;

public class TestBlockResponse {

    private Long id;
    private String name;
    private String description;
    private String expectedBehaviour;
    private String expectedRange;
    private String calibrationRequirement;
    private String safetyNotes;
    private String acceptanceCriteria;
    private String preconditions;
    private String dependencies;
    private String postConditions;
    private String passFailCriteria;
    private String expectedObservation;
    private String engineerRemarks;
    private Integer usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TestBlockResponse fromEntity(TestBlock b) {
        TestBlockResponse r = new TestBlockResponse();
        r.id = b.getId();
        r.name = b.getName();
        r.description = b.getDescription();
        r.expectedBehaviour = b.getExpectedBehaviour();
        r.expectedRange = b.getExpectedRange();
        r.calibrationRequirement = b.getCalibrationRequirement();
        r.safetyNotes = b.getSafetyNotes();
        r.acceptanceCriteria = b.getAcceptanceCriteria();
        r.preconditions = b.getPreconditions();
        r.dependencies = b.getDependencies();
        r.postConditions = b.getPostConditions();
        r.passFailCriteria = b.getPassFailCriteria();
        r.expectedObservation = b.getExpectedObservation();
        r.engineerRemarks = b.getEngineerRemarks();
        r.usageCount = b.getUsageCount();
        r.createdAt = b.getCreatedAt();
        r.updatedAt = b.getUpdatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getExpectedBehaviour() { return expectedBehaviour; }
    public String getExpectedRange() { return expectedRange; }
    public String getCalibrationRequirement() { return calibrationRequirement; }
    public String getSafetyNotes() { return safetyNotes; }
    public String getAcceptanceCriteria() { return acceptanceCriteria; }
    public String getPreconditions() { return preconditions; }
    public String getDependencies() { return dependencies; }
    public String getPostConditions() { return postConditions; }
    public String getPassFailCriteria() { return passFailCriteria; }
    public String getExpectedObservation() { return expectedObservation; }
    public String getEngineerRemarks() { return engineerRemarks; }
    public Integer getUsageCount() { return usageCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}