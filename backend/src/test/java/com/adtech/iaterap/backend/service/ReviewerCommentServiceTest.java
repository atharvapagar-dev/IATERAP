package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.ReviewerCommentRequest;
import com.adtech.iaterap.backend.dto.ReviewerCommentResponse;
import com.adtech.iaterap.backend.dto.TestStepResponse;
import com.adtech.iaterap.backend.entity.ProcedureHistory;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.entity.TestStep;
import com.adtech.iaterap.backend.repository.ProcedureHistoryRepository;
import com.adtech.iaterap.backend.repository.ReviewerCommentRepository;
import com.adtech.iaterap.backend.repository.TestStepRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * AI-174 / T6-B2: revision anchoring + PART W integrity rule (a comment
 * naming a TestStep must be anchored to a revision that step actually
 * belongs to).
 */
@ExtendWith(MockitoExtension.class)
class ReviewerCommentServiceTest {

    @Mock private ReviewerCommentRepository reviewerCommentRepository;
    @Mock private ProcedureHistoryRepository procedureHistoryRepository;
    @Mock private TestStepRepository testStepRepository;

    private ReviewerCommentService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new ReviewerCommentService(
                reviewerCommentRepository, procedureHistoryRepository, testStepRepository, objectMapper);
    }

    private static <T> T withId(T entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private String snapshotContaining(Long... stepIds) throws Exception {
        List<Object> nodes = List.of(stepIds).stream()
                .<Object>map(id -> java.util.Map.of("id", id))
                .toList();
        return objectMapper.writeValueAsString(nodes);
    }

    @Test
    void validCommentOnRevisionOnly() throws Exception {
        ProcedureHistory revision = withId(new ProcedureHistory(), 1L);
        revision.setTestProcedureId(10L);
        revision.setVersionNumber(1);
        revision.setStepsSnapshot(snapshotContaining(100L));

        when(procedureHistoryRepository.findById(1L)).thenReturn(Optional.of(revision));
        when(reviewerCommentRepository.save(any())).thenAnswer(inv -> {
            var c = inv.getArgument(0, com.adtech.iaterap.backend.entity.ReviewerComment.class);
            withId(c, 500L);
            return c;
        });

        ReviewerCommentRequest request = new ReviewerCommentRequest();
        request.setProcedureHistoryId(1L);
        request.setCommentText("Looks good overall.");

        ReviewerCommentResponse response = service.save(request);

        assertThat(response.getProcedureHistoryId()).isEqualTo(1L);
        assertThat(response.getTestStepId()).isNull();
        assertThat(response.getCommentText()).isEqualTo("Looks good overall.");
        verifyNoInteractions(testStepRepository);
    }

    @Test
    void validCommentOnRevisionAndMatchingTestStep() throws Exception {
        TestProcedure procedure = withId(new TestProcedure(), 10L);

        ProcedureHistory revision = withId(new ProcedureHistory(), 1L);
        revision.setTestProcedureId(10L);
        revision.setVersionNumber(2);
        revision.setStepsSnapshot(snapshotContaining(100L, 101L));

        TestStep step = withId(new TestStep(), 100L);
        step.setTestProcedure(procedure);

        when(procedureHistoryRepository.findById(1L)).thenReturn(Optional.of(revision));
        when(testStepRepository.findById(100L)).thenReturn(Optional.of(step));
        when(reviewerCommentRepository.save(any())).thenAnswer(inv -> {
            var c = inv.getArgument(0, com.adtech.iaterap.backend.entity.ReviewerComment.class);
            withId(c, 501L);
            return c;
        });

        ReviewerCommentRequest request = new ReviewerCommentRequest();
        request.setProcedureHistoryId(1L);
        request.setTestStepId(100L);
        request.setCommentText("Check the tolerance on this step.");

        ReviewerCommentResponse response = service.save(request);

        assertThat(response.getTestStepId()).isEqualTo(100L);
    }

    @Test
    void invalidStepNotBelongingToRevisionIsRejected() throws Exception {
        // Step belongs to a *different* TestProcedure than the revision's.
        TestProcedure otherProcedure = withId(new TestProcedure(), 999L);

        ProcedureHistory revision = withId(new ProcedureHistory(), 1L);
        revision.setTestProcedureId(10L);
        revision.setVersionNumber(2);
        revision.setStepsSnapshot(snapshotContaining(100L));

        TestStep foreignStep = withId(new TestStep(), 200L);
        foreignStep.setTestProcedure(otherProcedure);

        when(procedureHistoryRepository.findById(1L)).thenReturn(Optional.of(revision));
        when(testStepRepository.findById(200L)).thenReturn(Optional.of(foreignStep));

        ReviewerCommentRequest request = new ReviewerCommentRequest();
        request.setProcedureHistoryId(1L);
        request.setTestStepId(200L);
        request.setCommentText("Invalid anchor.");

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(reviewerCommentRepository, never()).save(any());
    }

    @Test
    void stepNotPresentInThisRevisionsSnapshotIsRejected() throws Exception {
        // Same procedure, but this step id was not part of THIS revision's snapshot
        // (e.g. it belongs only to a later or earlier revision).
        TestProcedure procedure = withId(new TestProcedure(), 10L);

        ProcedureHistory revision = withId(new ProcedureHistory(), 1L);
        revision.setTestProcedureId(10L);
        revision.setVersionNumber(1);
        revision.setStepsSnapshot(snapshotContaining(100L)); // only step 100 existed at revision 1

        TestStep laterStep = withId(new TestStep(), 101L); // added only in revision 2
        laterStep.setTestProcedure(procedure);

        when(procedureHistoryRepository.findById(1L)).thenReturn(Optional.of(revision));
        when(testStepRepository.findById(101L)).thenReturn(Optional.of(laterStep));

        ReviewerCommentRequest request = new ReviewerCommentRequest();
        request.setProcedureHistoryId(1L);
        request.setTestStepId(101L);
        request.setCommentText("Anchored to the wrong revision.");

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(reviewerCommentRepository, never()).save(any());
    }

    @Test
    void missingRevisionIsRejected() {
        when(procedureHistoryRepository.findById(999L)).thenReturn(Optional.empty());

        ReviewerCommentRequest request = new ReviewerCommentRequest();
        request.setProcedureHistoryId(999L);
        request.setCommentText("orphan comment");

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void missingTestStepReferenceIsRejected() {
        ProcedureHistory revision = withId(new ProcedureHistory(), 1L);
        revision.setTestProcedureId(10L);
        revision.setVersionNumber(1);
        revision.setStepsSnapshot("[]");

        when(procedureHistoryRepository.findById(1L)).thenReturn(Optional.of(revision));
        when(testStepRepository.findById(404L)).thenReturn(Optional.empty());

        ReviewerCommentRequest request = new ReviewerCommentRequest();
        request.setProcedureHistoryId(1L);
        request.setTestStepId(404L);
        request.setCommentText("dangling step reference");

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(NoSuchElementException.class);
    }
}
