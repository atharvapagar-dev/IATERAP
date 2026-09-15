package com.adtech.iaterap.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.LocalDateTime;

@Entity @Immutable @Table(name = "coverage_report")
public class CoverageReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="procedure_snapshot_id", nullable=false, updatable=false) private Long procedureSnapshotId;
    @Column(name="test_procedure_id", nullable=false, updatable=false) private Long testProcedureId;
    @Column(name="procedure_version_number", nullable=false, updatable=false) private Integer procedureVersionNumber;
    @Column(name="report_version", nullable=false, updatable=false) private Integer reportVersion = 1;
    @Column(name="report_payload", columnDefinition="TEXT", nullable=false, updatable=false) private String reportPayload;
    @Column(name="generated_at", nullable=false, updatable=false) private LocalDateTime generatedAt = LocalDateTime.now();
    protected CoverageReport() {}
    public CoverageReport(Long snapshotId, Long procedureId, Integer version, String payload) { procedureSnapshotId=snapshotId; testProcedureId=procedureId; procedureVersionNumber=version; reportPayload=payload; }
    public Long getId(){return id;} public Long getProcedureSnapshotId(){return procedureSnapshotId;} public Long getTestProcedureId(){return testProcedureId;} public Integer getProcedureVersionNumber(){return procedureVersionNumber;} public Integer getReportVersion(){return reportVersion;} public String getReportPayload(){return reportPayload;} public LocalDateTime getGeneratedAt(){return generatedAt;}
}
