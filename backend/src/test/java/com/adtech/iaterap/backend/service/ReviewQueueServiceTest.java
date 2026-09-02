package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.approval.ApprovalState;
import com.adtech.iaterap.backend.dto.TestProcedureResponse;
import com.adtech.iaterap.backend.entity.*;
import com.adtech.iaterap.backend.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * AI-191 / T8-B1: Review Queue backend query.
 * 
 * Verifies that:
 * - getReviewQueue() returns only IN_REVIEW procedures
 * - Empty results handled gracefully
 * - Each procedure includes its test steps
 * - Uses the repository's findByApprovalStatus with correct state
 */
@ExtendWith(MockitoExtension.class)
class ReviewQueueServiceTest {

    @Mock
    private TestProcedureRepository testProcedureRepository;
    @Mock
    private TestStepRepository testStepRepository;
    @Mock
    private ProcedureHistoryRepository procedureHistoryRepository;
    @Mock
    private LruRepository lruRepository;
    @Mock
    private InterfaceRepository interfaceRepository;
    @Mock
    private ConnectorRepository connectorRepository;
    @Mock
    private SignalLibraryRepository signalLibraryRepository;
    @Mock
    private ProtocolTagRepository protocolTagRepository;
    @Mock
    private FaultTemplateRepository faultTemplateRepository;
    @Mock
    private AuditService auditService;

    private TestProcedureService service;

    @BeforeEach
    void setUp() {
        service = new TestProcedureService(
                testProcedureRepository,
                testStepRepository,
                procedureHistoryRepository,
                lruRepository,
                interfaceRepository,
                connectorRepository,
                signalLibraryRepository,
                protocolTagRepository,
                faultTemplateRepository,
                new ObjectMapper().findAndRegisterModules(),
                auditService
        );
    }

    @Test
    void getReviewQueueReturnsOnlyInReviewProcedures() {
        Lru lru = new Lru();
        withId(lru, 1L);
        lru.setName("Test LRU");

        TestProcedure inReview1 = new TestProcedure();
        withId(inReview1, 100L);
        inReview1.setName("Proc 1");
        inReview1.setLru(lru);
        inReview1.setApprovalStatus(ApprovalState.IN_REVIEW.name());
        inReview1.setVersionNumber(1);

        TestProcedure inReview2 = new TestProcedure();
        withId(inReview2, 101L);
        inReview2.setName("Proc 2");
        inReview2.setLru(lru);
        inReview2.setApprovalStatus(ApprovalState.IN_REVIEW.name());
        inReview2.setVersionNumber(2);

        lenient().when(lruRepository.findById(1L)).thenReturn(java.util.Optional.of(lru));
        when(testProcedureRepository.findByApprovalStatus(ApprovalState.IN_REVIEW.name()))
                .thenReturn(List.of(inReview1, inReview2));
        when(testStepRepository.findByTestProcedureIdOrderByExecutionOrderAsc(100L))
                .thenReturn(List.of());
        when(testStepRepository.findByTestProcedureIdOrderByExecutionOrderAsc(101L))
                .thenReturn(List.of());

        List<TestProcedureResponse> queue = service.getReviewQueue();

        assertThat(queue).hasSize(2);
        assertThat(queue.get(0).getId()).isEqualTo(100L);
        assertThat(queue.get(0).getApprovalStatus()).isEqualTo("IN_REVIEW");
        assertThat(queue.get(1).getId()).isEqualTo(101L);
        assertThat(queue.get(1).getApprovalStatus()).isEqualTo("IN_REVIEW");
    }

    @Test
    void getReviewQueueReturnsEmptyListWhenNoProceduresInReview() {
        when(testProcedureRepository.findByApprovalStatus(ApprovalState.IN_REVIEW.name()))
                .thenReturn(List.of());

        List<TestProcedureResponse> queue = service.getReviewQueue();

        assertThat(queue).isEmpty();
    }

    @Test
    void getReviewQueueIncludesTestStepsForEachProcedure() {
        Lru lru = new Lru();
        withId(lru, 1L);
        lru.setName("Test LRU");

        TestProcedure inReview = new TestProcedure();
        withId(inReview, 100L);
        inReview.setName("Proc 1");
        inReview.setLru(lru);
        inReview.setApprovalStatus(ApprovalState.IN_REVIEW.name());
        inReview.setVersionNumber(1);

        TestStep step1 = new TestStep();
        withId(step1, 1000L);
        step1.setExecutionOrder(1);
        step1.setTestProcedure(inReview);

        TestStep step2 = new TestStep();
        withId(step2, 1001L);
        step2.setExecutionOrder(2);
        step2.setTestProcedure(inReview);

        lenient().when(lruRepository.findById(1L)).thenReturn(java.util.Optional.of(lru));
        when(testProcedureRepository.findByApprovalStatus(ApprovalState.IN_REVIEW.name()))
                .thenReturn(List.of(inReview));
        when(testStepRepository.findByTestProcedureIdOrderByExecutionOrderAsc(100L))
                .thenReturn(List.of(step1, step2));

        List<TestProcedureResponse> queue = service.getReviewQueue();

        assertThat(queue).hasSize(1);
        assertThat(queue.get(0).getSteps()).hasSize(2);
        assertThat(queue.get(0).getSteps().get(0).getId()).isEqualTo(1000L);
        assertThat(queue.get(0).getSteps().get(1).getId()).isEqualTo(1001L);
    }

    private static <T> T withId(T entity, Long id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
