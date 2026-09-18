package com.adtech.iaterap.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "procedure_snapshot", uniqueConstraints = @UniqueConstraint(name = "uq_procedure_snapshot_version", columnNames = {"test_procedure_id", "procedure_version_number"}))
public class ProcedureSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "test_procedure_id", nullable = false, updatable = false) private Long testProcedureId;
    @Column(name = "procedure_version_number", nullable = false, updatable = false) private Integer procedureVersionNumber;
    @Column(name = "approval_status", nullable = false, updatable = false) private String approvalStatus;
    @Column(name = "snapshot_payload", columnDefinition = "TEXT", nullable = false, updatable = false) private String snapshotPayload;
    @Column(name = "created_by", nullable = false, updatable = false) private String createdBy;
    @Column(name = "snapshot_at", nullable = false, updatable = false) private LocalDateTime snapshotAt = LocalDateTime.now();
    protected ProcedureSnapshot() {}
    public ProcedureSnapshot(Long procedureId, Integer version, String status, String payload, String actor) { testProcedureId=procedureId; procedureVersionNumber=version; approvalStatus=status; snapshotPayload=payload; createdBy=actor; }
    public Long getId(){return id;} public Long getTestProcedureId(){return testProcedureId;} public Integer getProcedureVersionNumber(){return procedureVersionNumber;} public String getApprovalStatus(){return approvalStatus;} public String getSnapshotPayload(){return snapshotPayload;} public String getCreatedBy(){return createdBy;} public LocalDateTime getSnapshotAt(){return snapshotAt;}
}
