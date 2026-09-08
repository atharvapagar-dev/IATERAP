package com.adtech.iaterap.backend.entity;

import com.adtech.iaterap.backend.validation.EngineeringUnit;
import com.adtech.iaterap.backend.validation.ValidationResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "validation_readings",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_validation_reading_step",
                columnNames = {"validation_run_id", "test_step_id"}
        )
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ValidationReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "validation_run_id", nullable = false)
    private Long validationRunId;

    @NotNull
    @Column(name = "test_step_id", nullable = false)
    private Long testStepId;

    @Column(name = "source_row_number")
    private Integer sourceRowNumber;
    @Column(name = "observed_value", precision = 30, scale = 10)
    private BigDecimal observedValue;
    @Enumerated(EnumType.STRING)
    @Column(name = "observed_unit", length = 32)
    private EngineeringUnit observedUnit;
    @Column(name = "normalized_value", precision = 30, scale = 10)
    private BigDecimal normalizedValue;
    @Column(name = "observed_behaviour", length = 1000)
    private String observedBehaviour;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ValidationResult result;

    @Column(precision = 30, scale = 10)
    private BigDecimal deviation;
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Long getValidationRunId() { return validationRunId; }
    public void setValidationRunId(Long v) { this.validationRunId = v; }
    public Long getTestStepId() { return testStepId; }
    public void setTestStepId(Long v) { this.testStepId = v; }
    public Integer getSourceRowNumber() { return sourceRowNumber; }
    public void setSourceRowNumber(Integer v) { this.sourceRowNumber = v; }
    public BigDecimal getObservedValue() { return observedValue; }
    public void setObservedValue(BigDecimal v) { this.observedValue = v; }
    public EngineeringUnit getObservedUnit() { return observedUnit; }
    public void setObservedUnit(EngineeringUnit v) { this.observedUnit = v; }
    public BigDecimal getNormalizedValue() { return normalizedValue; }
    public void setNormalizedValue(BigDecimal v) { this.normalizedValue = v; }
    public String getObservedBehaviour() { return observedBehaviour; }
    public void setObservedBehaviour(String v) { this.observedBehaviour = v; }
    public ValidationResult getResult() { return result; }
    public void setResult(ValidationResult v) { this.result = v; }
    public BigDecimal getDeviation() { return deviation; }
    public void setDeviation(BigDecimal v) { this.deviation = v; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime v) { this.recordedAt = v; }
}
