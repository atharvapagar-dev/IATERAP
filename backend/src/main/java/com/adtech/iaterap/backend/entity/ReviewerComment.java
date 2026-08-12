package com.adtech.iaterap.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * AI-174 / T6-B2: a Reviewer Comment anchored to a specific, immutable
 * {@link ProcedureHistory} revision and, optionally, a specific TestStep
 * within that revision's procedure.
 */
@Entity
@Table(name = "reviewer_comments")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ReviewerComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "procedure_history_id", nullable = false)
    private ProcedureHistory procedureHistory;

    @ManyToOne
    @JoinColumn(name = "test_step_id")
    private TestStep testStep;

    @NotBlank
    @Column(nullable = false)
    private String author;

    @NotBlank
    @Column(name = "comment_text", length = 2000, nullable = false)
    private String commentText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ReviewerComment() {}

    public Long getId() { return id; }
    public ProcedureHistory getProcedureHistory() { return procedureHistory; }
    public void setProcedureHistory(ProcedureHistory v) { this.procedureHistory = v; }
    public TestStep getTestStep() { return testStep; }
    public void setTestStep(TestStep v) { this.testStep = v; }
    public String getAuthor() { return author; }
    public void setAuthor(String v) { this.author = v; }
    public String getCommentText() { return commentText; }
    public void setCommentText(String v) { this.commentText = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
