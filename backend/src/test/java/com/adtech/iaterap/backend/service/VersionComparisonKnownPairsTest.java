package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.ChangeType;
import com.adtech.iaterap.backend.dto.FieldChangeResponse;
import com.adtech.iaterap.backend.dto.StepComparisonResponse;
import com.adtech.iaterap.backend.dto.VersionComparisonResponse;
import com.adtech.iaterap.backend.entity.ProcedureHistory;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.repository.ProcedureHistoryRepository;
import com.adtech.iaterap.backend.repository.TestProcedureRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * AI-186 / T7-QA: known revision-pair tests for the version diff engine.
 *
 * <p>The revision snapshots are deliberately hand-constructed. This keeps
 * the expected diff unambiguous and tests the comparison boundary directly,
 * independently of procedure-save persistence and fixture setup.</p>
 */
@ExtendWith(MockitoExtension.class)
class VersionComparisonKnownPairsTest {

    private static final long PROCEDURE_ID = 700L;

    @Mock private TestProcedureRepository testProcedureRepository;
    @Mock private ProcedureHistoryRepository procedureHistoryRepository;

    private VersionComparisonService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        service = new VersionComparisonService(
                testProcedureRepository, procedureHistoryRepository, objectMapper);

        TestProcedure procedure = new TestProcedure();
        procedure.setName("Known Pair Procedure");
        when(testProcedureRepository.findById(PROCEDURE_ID)).thenReturn(Optional.of(procedure));
    }

    @Test
    void knownPairReportsChangedUnchangedAddedAndRemovedSteps() {
        stubRevision(1, List.of(
                step(101L, 1, "Power-on", "0-5V"),
                step(102L, 2, "Check status", null),
                step(104L, 3, "Record serial", null)));
        stubRevision(2, List.of(
                step(101L, 1, "Power-on", "0-28V"),
                step(102L, 2, "Check status", null),
                step(103L, 3, "Check fault flag", null)));

        VersionComparisonResponse result = service.compare(PROCEDURE_ID, 1, 2);

        assertEquals("Known Pair Procedure", result.getProcedureName());
        assertEquals(1, result.getChangedStepCount());
        assertEquals(1, result.getUnchangedStepCount());
        assertEquals(1, result.getAddedStepCount());
        assertEquals(1, result.getRemovedStepCount());
        assertEquals(4, result.getSteps().size());

        StepComparisonResponse changed = result.getSteps().get(0);
        assertEquals(ChangeType.CHANGED, changed.getChangeType());
        assertEquals(1, changed.getFieldChanges().size());
        FieldChangeResponse range = changed.getFieldChanges().get(0);
        assertEquals("expectedRange", range.getFieldName());
        assertEquals("0-5V", range.getOldValue());
        assertEquals("0-28V", range.getNewValue());
        assertEquals(ChangeType.CHANGED, range.getChangeType());

        assertTrue(result.getSteps().stream().anyMatch(s -> s.getChangeType() == ChangeType.ADDED
                && s.getToStep() != null && s.getToStep().getId().equals(103L)));
        assertTrue(result.getSteps().stream().anyMatch(s -> s.getChangeType() == ChangeType.REMOVED
                && s.getFromStep() != null && s.getFromStep().getId().equals(104L)));
    }

    @Test
    void knownPairTreatsReorderingAsExecutionOrderChanges() {
        stubRevision(1, List.of(
                step(201L, 1, "Inspect A", null),
                step(202L, 2, "Inspect B", null)));
        stubRevision(2, List.of(
                step(202L, 1, "Inspect B", null),
                step(201L, 2, "Inspect A", null)));

        VersionComparisonResponse result = service.compare(PROCEDURE_ID, 1, 2);

        assertEquals(0, result.getAddedStepCount());
        assertEquals(0, result.getRemovedStepCount());
        assertEquals(2, result.getChangedStepCount());
        // The response is sorted by the effective (to-version) execution order.
        assertEquals(202L, result.getSteps().get(0).getToStep().getId());
        assertEquals(201L, result.getSteps().get(1).getToStep().getId());
        assertFieldChange(result.getSteps().get(0), "executionOrder", "2", "1");
        assertFieldChange(result.getSteps().get(1), "executionOrder", "1", "2");
    }

    @Test
    void knownPairNormalizesNullAndBlankOptionalValues() {
        stubRevision(1, List.of(step(301L, 1, "Observe signal", null)));
        stubRevision(2, List.of(step(301L, 1, "Observe signal", "")));

        VersionComparisonResponse result = service.compare(PROCEDURE_ID, 1, 2);

        assertEquals(0, result.getChangedStepCount());
        assertEquals(1, result.getUnchangedStepCount());
        assertTrue(result.getSteps().get(0).getFieldChanges().isEmpty());
    }

    @Test
    void missingKnownRevisionIsRejected() {
        stubRevision(1, List.of(step(401L, 1, "Baseline", null)));
        when(procedureHistoryRepository.findByTestProcedureIdAndVersionNumber(PROCEDURE_ID, 2))
                .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.compare(PROCEDURE_ID, 1, 2));
    }

    private void stubRevision(int version, List<Map<String, Object>> steps) {
        ProcedureHistory history = new ProcedureHistory();
        history.setTestProcedureId(PROCEDURE_ID);
        history.setVersionNumber(version);
        history.setStepsSnapshot(toJson(steps));

        when(procedureHistoryRepository.findByTestProcedureIdAndVersionNumber(PROCEDURE_ID, version))
                .thenReturn(Optional.of(history));
    }

    private Map<String, Object> step(Long id, int executionOrder, String behaviour, String expectedRange) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("id", id);
        step.put("executionOrder", executionOrder);
        step.put("expectedBehaviour", behaviour);
        step.put("expectedRange", expectedRange);
        return step;
    }

    private String toJson(List<Map<String, Object>> steps) {
        try {
            return new ObjectMapper().writeValueAsString(new ArrayList<>(steps));
        } catch (Exception exception) {
            throw new AssertionError("Could not build known revision snapshot", exception);
        }
    }

    private void assertFieldChange(StepComparisonResponse step, String field, String oldValue, String newValue) {
        FieldChangeResponse change = step.getFieldChanges().stream()
                .filter(candidate -> candidate.getFieldName().equals(field))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing field change: " + field));
        assertEquals(oldValue, change.getOldValue());
        assertEquals(newValue, change.getNewValue());
        assertEquals(ChangeType.CHANGED, change.getChangeType());
    }
}
