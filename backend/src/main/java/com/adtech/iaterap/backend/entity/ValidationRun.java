package com.adtech.iaterap.backend.entity;

import com.adtech.iaterap.backend.validation.ValidationRunStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "validation_runs",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_validation_run_source",
                columnNames = {"procedure_history_id", "source_sha256"}
        )
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ValidationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "procedure_history_id", nullable = false)
    private Long procedureHistoryId;

    @NotBlank
    @Column(name = "source_filename", nullable = false)
    private String sourceFilename;

    @NotBlank
    @Column(name = "source_sha256", length = 64, nullable = false)
    private String sourceSha256;

    @NotBlank
    @Column(name = "source_format", length = 16, nullable = false)
    private String sourceFormat;

    @NotBlank
    @Column(name = "recorded_by", nullable = false)
    private String recordedBy;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValidationRunStatus status = ValidationRunStatus.RECEIVED;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Column(name = "total_readings", nullable = false)
    private int totalReadings;
    @Column(name = "matched_count", nullable = false)
    private int matchedCount;
    @Column(name = "mismatched_count", nullable = false)
    private int mismatchedCount;
    @Column(name = "outlier_count", nullable = false)
    private int outlierCount;
    @Column(name = "missing_count", nullable = false)
    private int missingCount;

    public Long getId() { return id; }
    public Long getProcedureHistoryId() { return procedureHistoryId; }
    public void setProcedureHistoryId(Long v) { this.procedureHistoryId = v; }
    public String getSourceFilename() { return sourceFilename; }
    public void setSourceFilename(String v) { this.sourceFilename = v; }
    public String getSourceSha256() { return sourceSha256; }
    public void setSourceSha256(String v) { this.sourceSha256 = v; }
    public String getSourceFormat() { return sourceFormat; }
    public void setSourceFormat(String v) { this.sourceFormat = v; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String v) { this.recordedBy = v; }
    public ValidationRunStatus getStatus() { return status; }
    public void setStatus(ValidationRunStatus v) { this.status = v; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime v) { this.uploadedAt = v; }
    public int getTotalReadings() { return totalReadings; }
    public void setTotalReadings(int v) { this.totalReadings = v; }
    public int getMatchedCount() { return matchedCount; }
    public void setMatchedCount(int v) { this.matchedCount = v; }
    public int getMismatchedCount() { return mismatchedCount; }
    public void setMismatchedCount(int v) { this.mismatchedCount = v; }
    public int getOutlierCount() { return outlierCount; }
    public void setOutlierCount(int v) { this.outlierCount = v; }
    public int getMissingCount() { return missingCount; }
    public void setMissingCount(int v) { this.missingCount = v; }
}
