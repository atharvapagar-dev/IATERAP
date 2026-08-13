package com.adtech.iaterap.backend.dto;

import com.adtech.iaterap.backend.entity.ReviewerComment;

import java.time.LocalDateTime;

public class ReviewerCommentResponse {

    private Long id;
    private Long procedureHistoryId;
    private Integer procedureHistoryVersionNumber;
    private Long testStepId;
    private String author;
    private String commentText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewerCommentResponse fromEntity(ReviewerComment c) {
        ReviewerCommentResponse r = new ReviewerCommentResponse();
        r.id = c.getId();
        r.procedureHistoryId = c.getProcedureHistory().getId();
        r.procedureHistoryVersionNumber = c.getProcedureHistory().getVersionNumber();
        r.testStepId = c.getTestStep() != null ? c.getTestStep().getId() : null;
        r.author = c.getAuthor();
        r.commentText = c.getCommentText();
        r.createdAt = c.getCreatedAt();
        r.updatedAt = c.getUpdatedAt();
        return r;
    }

    public Long getId() { return id; }
    public Long getProcedureHistoryId() { return procedureHistoryId; }
    public Integer getProcedureHistoryVersionNumber() { return procedureHistoryVersionNumber; }
    public Long getTestStepId() { return testStepId; }
    public String getAuthor() { return author; }
    public String getCommentText() { return commentText; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
