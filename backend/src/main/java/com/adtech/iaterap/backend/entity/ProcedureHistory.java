package com.adtech.iaterap.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * AI-173 / T6-B1: an immutable snapshot of a TestProcedure (and its ordered
 * TestSteps) at the moment of a content-changing save.
 *
 * <p>Rows in this table are never updated after insert. {@code stepsSnapshot}
 * holds a JSON-serialized array of the procedure's TestSteps at save time,
 * so a historical revision remains byte-for-byte retrievable even after the
 * live {@code test_procedures}/{@code test_steps} rows are later changed --
 * this table does not merely point at the current mutable entity.</p>
 */
@Entity
@Table(name = "procedure_history")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProcedureHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "test_procedure_id", nullable = false)
    private Long testProcedureId;

    @NotNull
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @NotNull
    @Column(name = "lru_id", nullable = false)
    private Long lruId;

    @NotBlank
    @Column(name = "approval_status", nullable = false)
    private String approvalStatus;
    @NotBlank
    @Column(name = "steps_snapshot", columnDefinition = "TEXT", nullable = false)
    private String stepsSnapshot;

    @NotBlank
    @Column(name = "created_by", nullable = false)
    private String createdBy = "SYSTEM";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ProcedureHistory() {}

    public Long getId() { return id; }
    public Long getTestProcedureId() { return testProcedureId; }
    public void setTestProcedureId(Long v) { this.testProcedureId = v; }
    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer v) { this.versionNumber = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public Long getLruId() { return lruId; }
    public void setLruId(Long v) { this.lruId = v; }
    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String v) { this.approvalStatus = v; }
    public String getStepsSnapshot() { return stepsSnapshot; }
    public void setStepsSnapshot(String v) { this.stepsSnapshot = v; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { this.createdBy = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
