package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.approval.ApprovalStateMachine;
import com.adtech.iaterap.backend.approval.InvalidApprovalTransitionException;
import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.ProcedureHistory;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.entity.TestStep;
import com.adtech.iaterap.backend.repository.ConnectorRepository;
import com.adtech.iaterap.backend.repository.FaultTemplateRepository;
import com.adtech.iaterap.backend.repository.InterfaceRepository;
import com.adtech.iaterap.backend.repository.LruRepository;
import com.adtech.iaterap.backend.repository.ProcedureHistoryRepository;
import com.adtech.iaterap.backend.repository.ProtocolTagRepository;
import com.adtech.iaterap.backend.repository.SignalLibraryRepository;
import com.adtech.iaterap.backend.repository.TestProcedureRepository;
import com.adtech.iaterap.backend.repository.TestStepRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitForReviewServiceTest {

    @Mock private TestProcedureRepository testProcedureRepository;
    @Mock private TestStepRepository testStepRepository;
    @Mock private ProcedureHistoryRepository procedureHistoryRepository;
    @Mock private LruRepository lruRepository;
    @Mock private InterfaceRepository interfaceRepository;
    @Mock private ConnectorRepository connectorRepository;
    @Mock private SignalLibraryRepository signalLibraryRepository;
    @Mock private ProtocolTagRepository protocolTagRepository;
    @Mock private FaultTemplateRepository faultTemplateRepository;

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
                new ApprovalStateMachine()
        );
    }

    @Test
    void submitDraftWithBoundStepsTransitionsToInReview() {
        TestProcedure procedure = procedure(1L, "DRAFT");
        TestStep step = boundStep(10L, procedure);
        when(testProcedureRepository.findById(1L)).thenReturn(Optional.of(procedure));
        when(testStepRepository.findByTestProcedureIdOrderByExecutionOrderAsc(1L))
                .thenReturn(List.of(step));
        when(testProcedureRepository.save(procedure)).thenReturn(procedure);

        var response = service.submitForReview(1L);

        assertThat(response.getApprovalStatus()).isEqualTo("IN_REVIEW");
        assertThat(response.getVersionNumber()).isEqualTo(2);
        verify(testProcedureRepository).save(procedure);
        org.mockito.ArgumentCaptor<ProcedureHistory> history =
                org.mockito.ArgumentCaptor.forClass(ProcedureHistory.class);
        verify(procedureHistoryRepository, times(1)).save(history.capture());
        assertThat(history.getValue().getVersionNumber()).isEqualTo(2);
        assertThat(history.getValue().getApprovalStatus()).isEqualTo("IN_REVIEW");
    }

    @Test
    void submitWithoutStepsIsRejectedWithoutPersistingAStateChange() {
        TestProcedure procedure = procedure(1L, "DRAFT");
        when(testProcedureRepository.findById(1L)).thenReturn(Optional.of(procedure));
        when(testStepRepository.findByTestProcedureIdOrderByExecutionOrderAsc(1L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.submitForReview(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no Test Steps");

        assertThat(procedure.getApprovalStatus()).isEqualTo("DRAFT");
        verify(testProcedureRepository, never()).save(procedure);
    }

    @Test
    void submitWithAnUnboundStepIsRejectedWithoutPersistingAStateChange() {
        TestProcedure procedure = procedure(1L, "DRAFT");
        TestStep step = new TestStep();
        withId(step, 10L);
        step.setExecutionOrder(1);
        when(testProcedureRepository.findById(1L)).thenReturn(Optional.of(procedure));
        when(testStepRepository.findByTestProcedureIdOrderByExecutionOrderAsc(1L))
                .thenReturn(List.of(step));

        assertThatThrownBy(() -> service.submitForReview(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not bound to any");

        assertThat(procedure.getApprovalStatus()).isEqualTo("DRAFT");
        verify(testProcedureRepository, never()).save(procedure);
    }

    @Test
    void submitFromInReviewIsRejectedByTheStateMachine() {
        TestProcedure procedure = procedure(1L, "IN_REVIEW");
        TestStep step = boundStep(10L, procedure);
        when(testProcedureRepository.findById(1L)).thenReturn(Optional.of(procedure));
        when(testStepRepository.findByTestProcedureIdOrderByExecutionOrderAsc(1L))
                .thenReturn(List.of(step));

        assertThatThrownBy(() -> service.submitForReview(1L))
                .isInstanceOf(InvalidApprovalTransitionException.class)
                .hasMessageContaining("IN_REVIEW");

        verify(testProcedureRepository, never()).save(procedure);
    }

    private static TestProcedure procedure(Long id, String approvalStatus) {
        TestProcedure procedure = new TestProcedure();
        withId(procedure, id);
        procedure.setApprovalStatus(approvalStatus);
        procedure.setVersionNumber(1);
        Lru lru = new Lru();
        withId(lru, 100L);
        lru.setName("Test LRU");
        procedure.setLru(lru);
        return procedure;
    }

    private static TestStep boundStep(Long id, TestProcedure procedure) {
        TestStep step = new TestStep();
        withId(step, id);
        step.setTestProcedure(procedure);
        step.setExecutionOrder(1);
        step.setProtocolTag(new com.adtech.iaterap.backend.entity.ProtocolTag());
        return step;
    }

    private static <T> T withId(T entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }
}
