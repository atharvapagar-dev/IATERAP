package com.adtech.iaterap.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_suggestion_queue")
public class AiSuggestionQueueItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "test_procedure_id") private Long testProcedureId;
    @Column(name = "test_step_id") private Long testStepId;
    @Column(name = "suggestion_text", columnDefinition = "TEXT", nullable = false) private String suggestionText;
    @Column(nullable = false) private double confidence;
    private String source;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AiSuggestionStatus status = AiSuggestionStatus.PENDING;
    @Column(name = "requested_by", nullable = false) private String requestedBy;
    @Column(name = "decided_by") private String decidedBy;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "decided_at") private LocalDateTime decidedAt;
    public Long getId() { return id; }
    public Long getTestProcedureId() { return testProcedureId; } public void setTestProcedureId(Long value) { testProcedureId = value; }
    public Long getTestStepId() { return testStepId; } public void setTestStepId(Long value) { testStepId = value; }
    public String getSuggestionText() { return suggestionText; } public void setSuggestionText(String value) { suggestionText = value; }
    public double getConfidence() { return confidence; } public void setConfidence(double value) { confidence = value; }
    public String getSource() { return source; } public void setSource(String value) { source = value; }
    public AiSuggestionStatus getStatus() { return status; } public void setStatus(AiSuggestionStatus value) { status = value; }
    public String getRequestedBy() { return requestedBy; } public void setRequestedBy(String value) { requestedBy = value; }
    public String getDecidedBy() { return decidedBy; } public void setDecidedBy(String value) { decidedBy = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDecidedAt() { return decidedAt; } public void setDecidedAt(LocalDateTime value) { decidedAt = value; }
}
