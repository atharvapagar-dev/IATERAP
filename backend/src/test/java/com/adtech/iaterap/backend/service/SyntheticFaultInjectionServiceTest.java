package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.ProcedureHistory;
import com.adtech.iaterap.backend.entity.ValidationReading;
import com.adtech.iaterap.backend.entity.ValidationRun;
import com.adtech.iaterap.backend.repository.ProcedureHistoryRepository;
import com.adtech.iaterap.backend.repository.ValidationReadingRepository;
import com.adtech.iaterap.backend.repository.ValidationRunRepository;
import com.adtech.iaterap.backend.validation.SyntheticInjectionScenario;
import com.adtech.iaterap.backend.validation.ValidationComparisonService;
import com.adtech.iaterap.backend.validation.ValidationFixtureException;
import com.adtech.iaterap.backend.validation.ValidationFixtureParser;
import com.adtech.iaterap.backend.validation.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests verifying synthetic fault injection execution, snapshot immutability,
 * LRU targeting, and multi-category fault injection (T13-B1, T13-B2).
 */
class SyntheticFaultInjectionServiceTest {

    private ProcedureHistoryRepository historyRepository;
    private ValidationRunRepository runRepository;
    private ValidationReadingRepository readingRepository;
    private ValidationIngestionService ingestionService;
    private SyntheticFaultInjectionService faultInjectionService;
    private ObjectMapper objectMapper;

    private static final String TWO_STEPS_SNAPSHOT = """
            [
              {
                "id": 101,
                "executionOrder": 1,
                "expectedUnit": "VOLT",
                "expectedRange": "24.0..28.0",
                "expectedBehaviour": null
              },
              {
                "id": 102,
                "executionOrder": 2,
                "expectedUnit": null,
                "expectedRange": null,
                "expectedBehaviour": "STANDBY_READY"
              }
            ]
            """;

    @BeforeEach
    void setUp() {
        historyRepository = mock(ProcedureHistoryRepository.class);
        runRepository = mock(ValidationRunRepository.class);
        readingRepository = mock(ValidationReadingRepository.class);
        objectMapper = new ObjectMapper();

        ValidationFixtureParser parser = new ValidationFixtureParser(objectMapper);
        ValidationComparisonService comparisonService = new ValidationComparisonService();
        ingestionService = new ValidationIngestionService(
                historyRepository, runRepository, readingRepository, parser, comparisonService);

        faultInjectionService = new SyntheticFaultInjectionService(
                historyRepository, ingestionService, objectMapper);

        when(runRepository.save(any(ValidationRun.class))).thenAnswer(invocation -> {
            ValidationRun run = invocation.getArgument(0);
            return run;
        });
    }

    private ProcedureHistory createApprovedRevision(Long id, Long lruId, String stepsJson) {
        ProcedureHistory history = new ProcedureHistory();
        history.setTestProcedureId(42L);
        history.setVersionNumber(1);
        history.setName("Power Subsystem Verification");
        history.setLruId(lruId);
        history.setLruName("FADEC-MAIN");
        history.setApprovalStatus("APPROVED");
        history.setStepsSnapshot(stepsJson);
        history.setCreatedBy("janhavi");
        return history;
    }

    @Test
    @DisplayName("Nominal simulation generates matching readings for all steps")
    void nominalSimulationProducesMatchingReadings() {
        ProcedureHistory history = createApprovedRevision(1L, 10L, TWO_STEPS_SNAPSHOT);
        when(historyRepository.findById(1L)).thenReturn(Optional.of(history));

        ValidationRun run = faultInjectionService.run(1L, SyntheticInjectionScenario.NOMINAL, "VALIDATOR");

        assertNotNull(run);
        assertTrue(run.getSourceFilename().startsWith("synthetic-nominal-"));
        assertEquals(2, run.getTotalReadings());
        assertEquals(2, run.getMatchedCount());
        assertEquals(0, run.getMismatchedCount());
        assertEquals(0, run.getMissingCount());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ValidationReading>> readingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(readingRepository).saveAll(readingsCaptor.capture());
        List<ValidationReading> savedReadings = readingsCaptor.getValue();
        assertEquals(2, savedReadings.size());
        assertTrue(savedReadings.stream().allMatch(r -> r.getResult() == ValidationResult.MATCH));
    }

    @Test
    @DisplayName("OUT_OF_RANGE fault injection produces MISMATCH on numeric step")
    void outOfRangeFaultInjectionMutatesNumericStep() {
        ProcedureHistory history = createApprovedRevision(1L, 10L, TWO_STEPS_SNAPSHOT);
        when(historyRepository.findById(1L)).thenReturn(Optional.of(history));

        ValidationRun run = faultInjectionService.run(
                1L, SyntheticInjectionScenario.FAULT_INJECTED, "OUT_OF_RANGE", 10L, 101L, "VALIDATOR");

        assertNotNull(run);
        assertTrue(run.getSourceFilename().startsWith("synthetic-fault_injected-out_of_range-"));
        assertEquals(2, run.getTotalReadings());
        assertEquals(1, run.getMatchedCount());
        assertEquals(1, run.getMismatchedCount());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ValidationReading>> readingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(readingRepository).saveAll(readingsCaptor.capture());
        List<ValidationReading> savedReadings = readingsCaptor.getValue();

        ValidationReading step101 = savedReadings.stream().filter(r -> r.getTestStepId() == 101L).findFirst().orElseThrow();
        assertEquals(ValidationResult.MISMATCH, step101.getResult());
        assertNotNull(step101.getDeviation());
        assertTrue(step101.getDeviation().doubleValue() > 0);

        ValidationReading step102 = savedReadings.stream().filter(r -> r.getTestStepId() == 102L).findFirst().orElseThrow();
        assertEquals(ValidationResult.MATCH, step102.getResult());
    }

    @Test
    @DisplayName("DISCRETE_MISMATCH fault injection produces MISMATCH on behavioural step")
    void discreteMismatchFaultInjectionMutatesBehaviouralStep() {
        ProcedureHistory history = createApprovedRevision(1L, 10L, TWO_STEPS_SNAPSHOT);
        when(historyRepository.findById(1L)).thenReturn(Optional.of(history));

        ValidationRun run = faultInjectionService.run(
                1L, SyntheticInjectionScenario.FAULT_INJECTED, "DISCRETE_MISMATCH", 10L, 102L, "VALIDATOR");

        assertNotNull(run);
        assertTrue(run.getSourceFilename().startsWith("synthetic-fault_injected-discrete_mismatch-"));
        assertEquals(2, run.getTotalReadings());
        assertEquals(1, run.getMatchedCount());
        assertEquals(1, run.getMismatchedCount());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ValidationReading>> readingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(readingRepository).saveAll(readingsCaptor.capture());
        List<ValidationReading> savedReadings = readingsCaptor.getValue();

        ValidationReading step102 = savedReadings.stream().filter(r -> r.getTestStepId() == 102L).findFirst().orElseThrow();
        assertEquals(ValidationResult.MISMATCH, step102.getResult());
        assertEquals("SYNTHETIC_FAULT_DISCRETE_MISMATCH", step102.getObservedBehaviour());
    }

    @Test
    @DisplayName("MISSING_SIGNAL fault injection omits step reading, producing MISSING outcome")
    void missingSignalFaultInjectionOmitsReading() {
        ProcedureHistory history = createApprovedRevision(1L, 10L, TWO_STEPS_SNAPSHOT);
        when(historyRepository.findById(1L)).thenReturn(Optional.of(history));

        ValidationRun run = faultInjectionService.run(
                1L, SyntheticInjectionScenario.FAULT_INJECTED, "MISSING_SIGNAL", 10L, 101L, "VALIDATOR");

        assertNotNull(run);
        assertTrue(run.getSourceFilename().startsWith("synthetic-fault_injected-missing_signal-"));
        assertEquals(2, run.getTotalReadings());
        assertEquals(1, run.getMatchedCount());
        assertEquals(1, run.getMissingCount());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ValidationReading>> readingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(readingRepository).saveAll(readingsCaptor.capture());
        List<ValidationReading> savedReadings = readingsCaptor.getValue();

        ValidationReading step101 = savedReadings.stream().filter(r -> r.getTestStepId() == 101L).findFirst().orElseThrow();
        assertEquals(ValidationResult.MISSING, step101.getResult());
    }

    @Test
    @DisplayName("LRU mismatch is rejected with ValidationFixtureException")
    void lruMismatchThrowsValidationFixtureException() {
        ProcedureHistory history = createApprovedRevision(1L, 10L, TWO_STEPS_SNAPSHOT);
        when(historyRepository.findById(1L)).thenReturn(Optional.of(history));

        ValidationFixtureException ex = assertThrows(
                ValidationFixtureException.class,
                () -> faultInjectionService.run(1L, SyntheticInjectionScenario.NOMINAL, null, 999L, null, "VALIDATOR")
        );
        assertTrue(ex.getMessage().contains("does not belong to LRU 999"));
    }

    @Test
    @DisplayName("DRAFT procedure revision is rejected for synthetic fault injection")
    void draftProcedureThrowsValidationFixtureException() {
        ProcedureHistory history = createApprovedRevision(1L, 10L, TWO_STEPS_SNAPSHOT);
        history.setApprovalStatus("DRAFT");
        when(historyRepository.findById(1L)).thenReturn(Optional.of(history));

        ValidationFixtureException ex = assertThrows(
                ValidationFixtureException.class,
                () -> faultInjectionService.run(1L, SyntheticInjectionScenario.NOMINAL, "VALIDATOR")
        );
        assertTrue(ex.getMessage().contains("APPROVED"));
    }

    @Test
    @DisplayName("IN_REVIEW procedure revision is rejected for synthetic fault injection")
    void inReviewProcedureThrowsValidationFixtureException() {
        ProcedureHistory history = createApprovedRevision(1L, 10L, TWO_STEPS_SNAPSHOT);
        history.setApprovalStatus("IN_REVIEW");
        when(historyRepository.findById(1L)).thenReturn(Optional.of(history));

        ValidationFixtureException ex = assertThrows(
                ValidationFixtureException.class,
                () -> faultInjectionService.run(1L, SyntheticInjectionScenario.NOMINAL, "VALIDATOR")
        );
        assertTrue(ex.getMessage().contains("APPROVED"));
    }

    @Test
    @DisplayName("Missing revision ID throws ValidationFixtureException")
    void missingRevisionThrowsValidationFixtureException() {
        ValidationFixtureException ex = assertThrows(
                ValidationFixtureException.class,
                () -> faultInjectionService.run(null, SyntheticInjectionScenario.NOMINAL, "VALIDATOR")
        );
        assertTrue(ex.getMessage().contains("Procedure revision is required"));
    }

    @Test
    @DisplayName("Procedure with no steps throws ValidationFixtureException")
    void noStepsThrowsValidationFixtureException() {
        ProcedureHistory history = createApprovedRevision(1L, 10L, "[]");
        when(historyRepository.findById(1L)).thenReturn(Optional.of(history));

        ValidationFixtureException ex = assertThrows(
                ValidationFixtureException.class,
                () -> faultInjectionService.run(1L, SyntheticInjectionScenario.NOMINAL, "VALIDATOR")
        );
        assertTrue(ex.getMessage().contains("has no test steps"));
    }

    @Test
    @DisplayName("Unsupported fault category throws ValidationFixtureException")
    void unsupportedFaultCategoryThrowsValidationFixtureException() {
        ProcedureHistory history = createApprovedRevision(1L, 10L, TWO_STEPS_SNAPSHOT);
        when(historyRepository.findById(1L)).thenReturn(Optional.of(history));

        ValidationFixtureException ex = assertThrows(
                ValidationFixtureException.class,
                () -> faultInjectionService.run(1L, SyntheticInjectionScenario.FAULT_INJECTED, "RANDOM_INVALID", null, null, "VALIDATOR")
        );
        assertTrue(ex.getMessage().contains("Unsupported synthetic fault category"));
    }

    @Test
    @DisplayName("Target step not belonging to revision throws ValidationFixtureException")
    void targetStepNotInRevisionThrowsValidationFixtureException() {
        ProcedureHistory history = createApprovedRevision(1L, 10L, TWO_STEPS_SNAPSHOT);
        when(historyRepository.findById(1L)).thenReturn(Optional.of(history));

        ValidationFixtureException ex = assertThrows(
                ValidationFixtureException.class,
                () -> faultInjectionService.run(1L, SyntheticInjectionScenario.FAULT_INJECTED, "OUT_OF_RANGE", null, 9999L, "VALIDATOR")
        );
        assertTrue(ex.getMessage().contains("testStepId 9999 is not part of procedure revision"));
    }
}
