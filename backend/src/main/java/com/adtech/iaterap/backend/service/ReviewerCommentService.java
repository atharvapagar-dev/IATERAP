package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.ReviewerCommentRequest;
import com.adtech.iaterap.backend.dto.ReviewerCommentResponse;
import com.adtech.iaterap.backend.entity.ProcedureHistory;
import com.adtech.iaterap.backend.entity.ReviewerComment;
import com.adtech.iaterap.backend.entity.TestStep;
import com.adtech.iaterap.backend.repository.ProcedureHistoryRepository;
import com.adtech.iaterap.backend.repository.ReviewerCommentRepository;
import com.adtech.iaterap.backend.repository.TestStepRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * AI-174 / T6-B2: Reviewer Comments.
 *
 * <p>PART W integrity rule: a comment that names a TestStep must be anchored
 * to a revision that TestStep actually belongs to. Because TestStep rows are
 * live/mutable (only the procedure_history snapshot is immutable), "belongs
 * to" is checked two ways: (1) the step's *current* TestProcedure still
 * matches the revision's TestProcedure, and (2) the step's id is present in
 * that specific revision's frozen steps_snapshot -- i.e. the step existed,
 * with that identity, at the moment this revision was captured.</p>
 */
@Service
public class ReviewerCommentService {

    private final ReviewerCommentRepository reviewerCommentRepository;
    private final ProcedureHistoryRepository procedureHistoryRepository;
    private final TestStepRepository testStepRepository;
    private final ObjectMapper objectMapper;

    public ReviewerCommentService(
            ReviewerCommentRepository reviewerCommentRepository,
            ProcedureHistoryRepository procedureHistoryRepository,
            TestStepRepository testStepRepository,
            ObjectMapper objectMapper
    ) {
        this.reviewerCommentRepository = reviewerCommentRepository;
        this.procedureHistoryRepository = procedureHistoryRepository;
        this.testStepRepository = testStepRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ReviewerCommentResponse save(ReviewerCommentRequest request) {
        ProcedureHistory revision = procedureHistoryRepository.findById(request.getProcedureHistoryId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Procedure revision not found: " + request.getProcedureHistoryId()));

        ReviewerComment comment = new ReviewerComment();
        comment.setProcedureHistory(revision);
        comment.setAuthor(resolveActor());
        comment.setCommentText(request.getCommentText());

        if (request.getTestStepId() != null) {
            TestStep step = testStepRepository.findById(request.getTestStepId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Test step not found: " + request.getTestStepId()));

            validateStepBelongsToRevision(step, revision);
            comment.setTestStep(step);
        }

        comment = reviewerCommentRepository.save(comment);
        return ReviewerCommentResponse.fromEntity(comment);
    }

    public List<ReviewerCommentResponse> getByRevision(Long procedureHistoryId) {
        procedureHistoryRepository.findById(procedureHistoryId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Procedure revision not found: " + procedureHistoryId));

        return reviewerCommentRepository.findByProcedureHistoryIdOrderByCreatedAtAsc(procedureHistoryId).stream()
                .map(ReviewerCommentResponse::fromEntity)
                .toList();
    }

    private void validateStepBelongsToRevision(TestStep step, ProcedureHistory revision) {
        Long stepProcedureId = step.getTestProcedure() != null ? step.getTestProcedure().getId() : null;

        if (stepProcedureId == null || !stepProcedureId.equals(revision.getTestProcedureId())) {
            throw new IllegalArgumentException(
                    "Test step " + step.getId() + " does not belong to the same procedure as revision "
                            + revision.getVersionNumber() + " of procedure " + revision.getTestProcedureId());
        }

        if (!stepAppearsInSnapshot(step.getId(), revision.getStepsSnapshot())) {
            throw new IllegalArgumentException(
                    "Test step " + step.getId() + " is not part of revision "
                            + revision.getVersionNumber() + " of procedure " + revision.getTestProcedureId());
        }
    }

    private boolean stepAppearsInSnapshot(Long stepId, String stepsSnapshotJson) {
        try {
            JsonNode array = objectMapper.readTree(stepsSnapshotJson);
            for (JsonNode node : array) {
                if (node.has("id") && node.get("id").asLong() == stepId) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            // A malformed/unreadable snapshot cannot prove containment.
            return false;
        }
    }

    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getName() != null) {
            return authentication.getName();
        }
        return "SYSTEM";
    }
}
