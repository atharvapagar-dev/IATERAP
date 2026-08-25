package com.adtech.iaterap.backend.dto;

import com.adtech.iaterap.backend.entity.ProcedureHistory;

import java.time.LocalDateTime;

/**
 * Response shape for a retrieved procedure revision. {@code stepsSnapshot}
 * is returned as a raw JSON string (already serialized at write time) so
 * this DTO does not need to know the shape of the step snapshot to pass
 * it through faithfully.
 */
public class ProcedureHistoryResponse {

    private Long id;
    private Long testProcedureId;
    private Integer versionNumber;
    private String name;
    private String description;
    private Long lruId;
    private String approvalStatus;
    private String stepsSnapshot;
    private String createdBy;
    private LocalDateTime createdAt;

    public static ProcedureHistoryResponse fromEntity(ProcedureHistory h) {
        ProcedureHistoryResponse r = new ProcedureHistoryResponse();
        r.id = h.getId();
        r.testProcedureId = h.getTestProcedureId();
        r.versionNumber = h.getVersionNumber();
        r.name = h.getName();
        r.description = h.getDescription();
        r.lruId = h.getLruId();
        r.approvalStatus = h.getApprovalStatus();
        r.stepsSnapshot = h.getStepsSnapshot();
        r.createdBy = h.getCreatedBy();
        r.createdAt = h.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public Long getTestProcedureId() { return testProcedureId; }
    public Integer getVersionNumber() { return versionNumber; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Long getLruId() { return lruId; }
    public String getApprovalStatus() { return approvalStatus; }
    public String getStepsSnapshot() { return stepsSnapshot; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
