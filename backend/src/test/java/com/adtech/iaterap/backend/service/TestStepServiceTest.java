package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.Interface;
import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.entity.TestStep;
import com.adtech.iaterap.backend.repository.TestProcedureRepository;
import com.adtech.iaterap.backend.repository.TestStepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestStepServiceTest {

    @Mock
    private TestStepRepository repository;

    @Mock
    private TestProcedureRepository testProcedureRepository;

    private TestStepService service;

    @BeforeEach
    void setUp() {
        service = new TestStepService(repository, testProcedureRepository);
    }

    private TestStep stepWithId(Long id, int executionOrder) {
        TestStep step = new TestStep();
        step.setExecutionOrder(executionOrder);
        try {
            var idField = TestStep.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(step, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return step;
    }

    @Test
    void shouldReturnStepsByProcedureIdOrderedByExecutionOrder() {
        TestStep step = stepWithId(1L, 1);
        when(repository.findByTestProcedureIdOrderByExecutionOrderAsc(5L)).thenReturn(List.of(step));

        List<TestStep> result = service.getByProcedureId(5L);

        assertEquals(1, result.size());
        verify(repository, times(1)).findByTestProcedureIdOrderByExecutionOrderAsc(5L);
    }

    @Test
    void shouldReturnStepById() {
        TestStep step = stepWithId(1L, 1);
        when(repository.findById(1L)).thenReturn(Optional.of(step));

        TestStep result = service.getById(1L);

        assertEquals(1, result.getExecutionOrder());
        verify(repository, times(1)).findById(1L);
    }

    @Test
    void shouldThrowWhenStepDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> service.getById(99L)
        );

        assertEquals("Test step not found: 99", exception.getMessage());
    }

    @Test
    void shouldThrowWhenAddingStepToNonExistentProcedure() {
        when(testProcedureRepository.findById(404L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> service.addStep(404L, new TestStep())
        );

        assertEquals("Test procedure not found: 404", exception.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void shouldAssignExecutionOrderOneWhenProcedureHasNoExistingSteps() {
        TestProcedure procedure = new TestProcedure();
        Interface iface = new Interface("CAN", new Lru("Flight Control Unit"));

        when(testProcedureRepository.findById(1L)).thenReturn(Optional.of(procedure));
        when(repository.findByTestProcedureIdOrderByExecutionOrderAsc(1L)).thenReturn(Collections.emptyList());

        TestStep newStep = new TestStep();
        newStep.setInterfaceEntity(iface);
        newStep.setExpectedBehaviour("Signal present");
        newStep.setAcceptanceCriteria("Within range");
        newStep.setPassFailCriteria("Pass if within range");

        when(repository.save(any(TestStep.class))).thenAnswer(inv -> inv.getArgument(0));

        TestStep result = service.addStep(1L, newStep);

        assertEquals(1, result.getExecutionOrder());
        assertEquals("DRAFT", result.getApprovalStatus());
        assertSame(procedure, result.getTestProcedure());
        assertEquals(iface, result.getInterfaceEntity());
    }

    @Test
    void shouldAssignNextExecutionOrderWhenStepsAlreadyExist() {
        TestProcedure procedure = new TestProcedure();
        TestStep existingStep = stepWithId(1L, 3);

        when(testProcedureRepository.findById(1L)).thenReturn(Optional.of(procedure));
        when(repository.findByTestProcedureIdOrderByExecutionOrderAsc(1L))
                .thenReturn(List.of(existingStep));
        when(repository.save(any(TestStep.class))).thenAnswer(inv -> inv.getArgument(0));

        TestStep newStep = new TestStep();
        newStep.setExpectedBehaviour("Next step");
        newStep.setAcceptanceCriteria("Criteria");
        newStep.setPassFailCriteria("Pass/fail");

        TestStep result = service.addStep(1L, newStep);

        assertEquals(4, result.getExecutionOrder());
    }

    @Test
    void shouldUpdateStepContentFieldsWithoutTouchingExecutionOrderOrProcedure() {
        TestStep existing = stepWithId(1L, 2);
        TestProcedure originalProcedure = new TestProcedure();
        existing.setTestProcedure(originalProcedure);
        existing.setExpectedBehaviour("Old behaviour");

        TestStep incoming = new TestStep();
        incoming.setExpectedBehaviour("New behaviour");
        incoming.setAcceptanceCriteria("New acceptance");
        incoming.setPassFailCriteria("New pass/fail");

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        TestStep result = service.updateStep(1L, incoming);

        assertEquals("New behaviour", result.getExpectedBehaviour());
        assertEquals("New acceptance", result.getAcceptanceCriteria());
        assertEquals(2, result.getExecutionOrder(), "executionOrder must not change via updateStep");
        assertSame(originalProcedure, result.getTestProcedure(), "testProcedure must not change via updateStep");
    }

    @Test
    void shouldReorderStepsSequentiallyByGivenIdOrder() {
        TestStep stepA = stepWithId(1L, 1);
        TestStep stepB = stepWithId(2L, 2);
        TestStep stepC = stepWithId(3L, 3);

        when(repository.findByTestProcedureIdOrderByExecutionOrderAsc(10L))
                .thenReturn(List.of(stepA, stepB, stepC));
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<TestStep> result = service.reorderSteps(10L, List.of(3L, 1L, 2L));

        assertEquals(1, stepC.getExecutionOrder());
        assertEquals(2, stepA.getExecutionOrder());
        assertEquals(3, stepB.getExecutionOrder());
        assertEquals(3, result.size());
    }

    @Test
    void shouldRejectReorderWhenIdListSizeDoesNotMatchStepCount() {
        TestStep stepA = stepWithId(1L, 1);
        TestStep stepB = stepWithId(2L, 2);

        when(repository.findByTestProcedureIdOrderByExecutionOrderAsc(10L))
                .thenReturn(List.of(stepA, stepB));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.reorderSteps(10L, List.of(1L))
        );

        assertTrue(exception.getMessage().contains("does not match"));
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void shouldRejectReorderWhenIdDoesNotBelongToProcedure() {
        TestStep stepA = stepWithId(1L, 1);
        TestStep stepB = stepWithId(2L, 2);

        when(repository.findByTestProcedureIdOrderByExecutionOrderAsc(10L))
                .thenReturn(List.of(stepA, stepB));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.reorderSteps(10L, List.of(1L, 999L))
        );

        assertTrue(exception.getMessage().contains("does not belong to procedure"));
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void shouldDeleteStepWhenItExists() {
        TestStep step = stepWithId(1L, 1);
        when(repository.findById(1L)).thenReturn(Optional.of(step));

        service.deleteStep(1L);

        verify(repository, times(1)).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentStep() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.deleteStep(99L));
        verify(repository, never()).deleteById(anyLong());
    }
}
