package com.adtech.iaterap.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReviewerCommentRequest {

    @NotNull
    private Long procedureHistoryId;

    /** Optional: anchors the comment to a specific TestStep within that revision. */
    private Long testStepId;

    @NotBlank
    @Size(max = 2000)
    private String commentText;

    public Long getProcedureHistoryId() { return procedureHistoryId; }
    public void setProcedureHistoryId(Long v) { this.procedureHistoryId = v; }
    public Long getTestStepId() { return testStepId; }
    public void setTestStepId(Long v) { this.testStepId = v; }
    public String getCommentText() { return commentText; }
    public void setCommentText(String v) { this.commentText = v; }
}
