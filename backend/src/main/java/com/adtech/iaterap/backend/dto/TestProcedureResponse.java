package com.adtech.iaterap.backend.dto;

import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.entity.TestStep;

import java.time.LocalDateTime;
import java.util.List;

public class TestProcedureResponse {

    private Long id;
    private Long lruId;
    private String name;
    private String description;
    private String approvalStatus;
    private Integer versionNumber;
    private List<TestStepResponse> steps;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TestProcedureResponse fromEntity(TestProcedure p, List<TestStep> orderedSteps) {
        TestProcedureResponse r = new TestProcedureResponse();
        r.id = p.getId();
        r.lruId = p.getLru() != null ? p.getLru().getId() : null;
        r.name = p.getName();
        r.description = p.getDescription();
        r.approvalStatus = p.getApprovalStatus();
        r.versionNumber = p.getVersionNumber();
        r.steps = orderedSteps.stream().map(TestStepResponse::fromEntity).toList();
        r.createdAt = p.getCreatedAt();
        r.updatedAt = p.getUpdatedAt();
        return r;
    }

    public Long getId() { return id; }
    public Long getLruId() { return lruId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getApprovalStatus() { return approvalStatus; }
    public Integer getVersionNumber() { return versionNumber; }
    public List<TestStepResponse> getSteps() { return steps; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
