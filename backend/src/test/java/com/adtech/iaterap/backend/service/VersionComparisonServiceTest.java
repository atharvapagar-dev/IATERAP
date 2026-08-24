package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.ChangeType;
import com.adtech.iaterap.backend.dto.FieldChangeResponse;
import com.adtech.iaterap.backend.dto.StepComparisonResponse;
import com.adtech.iaterap.backend.dto.TestProcedureRequest;
import com.adtech.iaterap.backend.dto.TestProcedureResponse;
import com.adtech.iaterap.backend.dto.TestStepRequest;
import com.adtech.iaterap.backend.dto.VersionComparisonResponse;
import com.adtech.iaterap.backend.entity.*;
import com.adtech.iaterap.backend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI-182 / T7-B1: Version Comparison engine tests.
 *
 * <p>Follows the same integration-style convention as
 * {@link TwoRevisionRetrievabilityTest}: real repositories against the
 * synthetic-data test database, driving revisions through the actual
 * {@link TestProcedureService} save/update flow rather than hand-inserting
 * ProcedureHistory rows, so these tests exercise the real snapshot content
 * the comparison engine will see in production.</p>
 */
@SpringBootTest
@ActiveProfiles("synthetic-data")
class VersionComparisonServiceTest {

    @Autowired private VersionComparisonService versionComparisonService;
    @Autowired private TestProcedureService testProcedureService;
    @Autowired private TestProcedureRepository testProcedureRepository;
    @Autowired private TestStepRepository testStepRepository;
    @Autowired private LruRepository lruRepository;
    @Autowired private InterfaceRepository interfaceRepository;
    @Autowired private ConnectorRepository connectorRepository;
    @Autowired private SignalLibraryRepository signalLibraryRepository;
    @Autowired private ProtocolTagRepository protocolTagRepository;
    @Autowired private FaultTemplateRepository faultTemplateRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final List<Long> createdProcedureIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long procedureId : createdProcedureIds) {
            jdbcTemplate.update("DELETE FROM procedure_history WHERE test_procedure_id = ?", procedureId);
            testStepRepository.deleteAll(testStepRepository.findByTestProcedureIdOrderByExecutionOrderAsc(procedureId));
            testProcedureRepository.deleteById(procedureId);
        }
        createdProcedureIds.clear();
    }

    // ---------- fixtures (mirrors TwoRevisionRetrievabilityTest) ----------

    private TestStepRequest buildStepRequest(Long id, String behaviour, String acceptanceCriteria, int order) {
        Lru lru = lruRepository.findAll().get(0);
        Interface iface = interfaceRepository.findAll().stream()
                .filter(i -> i.getLru().getId().equals(lru.getId()))
                .findFirst()
                .orElseThrow();
        Connector connector = connectorRepository.findAll().get(0);
        SignalLibrary signal = signalLibraryRepository.findAll().get(0);
        ProtocolTag protocolTag = protocolTagRepository.findAll().get(0);
        FaultTemplate fault = faultTemplateRepository.findAll().get(0);

        TestStepRequest step = new TestStepRequest();
        step.setId(id);
        step.setInterfaceId(iface.getId());
        step.setConnectorId(connector.getId());
        step.setSignalLibraryId(signal.getId());
        step.setProtocolTagId(protocolTag.getId());
        step.setFaultCategoryId(fault.getId());
        step.setExpectedBehaviour(behaviour);
        step.setAcceptanceCriteria(acceptanceCriteria);
        step.setPassFailCriteria("pass/fail");
        step.setExecutionOrder(order);
        return step;
    }

    private TestStepRequest buildStepRequest(String behaviour, int order) {
        return buildStepRequest(null, behaviour, "criteria", order);
    }

    private TestProcedureRequest buildProcedureRequest(String name, List<TestStepRequest> steps) {
        Lru lru = lruRepository.findAll().get(0);
        TestProcedureRequest request = new TestProcedureRequest();
        request.setLruId(lru.getId());
        request.setName(name);
        request.setDescription("AI-182 version comparison fixture");
        request.setSteps(steps);
        return request;
    }

    private TestProcedureResponse createProcedure(String name, List<TestStepRequest> steps) {
        TestProcedureResponse created = testProcedureService.save(buildProcedureRequest(name, steps));
        createdProcedureIds.add(created.getId());
        return created;
    }

    private FieldChangeResponse findField(StepComparisonResponse step, String fieldName) {
        return step.getFieldChanges().stream()
                .filter(f -> f.getFieldName().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected field change for '" + fieldName + "' but found none"));
    }

    // ---------- 1. same revision compared with itself ----------

    @Test
    void comparingARevisionWithItselfReportsEverythingUnchanged() {
        TestProcedureResponse created = createProcedure(
                "AI-182 Self Compare",
                List.of(buildStepRequest("behaviour v1", 1)));

        VersionComparisonResponse comparison =
                versionComparisonService.compare(created.getId(), 1, 1);

        assertEquals(1, comparison.getSteps().size());
        assertEquals(ChangeType.UNCHANGED, comparison.getSteps().get(0).getChangeType());
        assertTrue(comparison.getSteps().get(0).getFieldChanges().isEmpty());
        assertEquals(0, comparison.getAddedStepCount());
        assertEquals(0, comparison.getRemovedStepCount());
        assertEquals(0, comparison.getChangedStepCount());
        assertEquals(1, comparison.getUnchangedStepCount());
        assertEquals(1, comparison.getFromVersion());
        assertEquals(1, comparison.getToVersion());
    }

    // ---------- 2. one changed field ----------

    @Test
    void oneChangedFieldIsReportedOnTheMatchedStep() {
        TestProcedureResponse created = createProcedure(
                "AI-182 One Field Change",
                List.of(buildStepRequest("behaviour v1", 1)));
        Long stepId = created.getSteps().get(0).getId();

        testProcedureService.update(created.getId(), buildProcedureRequest(
                "AI-182 One Field Change",
                List.of(buildStepRequest(stepId, "behaviour v2", "criteria", 1))));

        VersionComparisonResponse comparison = versionComparisonService.compare(created.getId(), 1, 2);

        assertEquals(1, comparison.getSteps().size());
        StepComparisonResponse step = comparison.getSteps().get(0);
        assertEquals(ChangeType.CHANGED, step.getChangeType());
        assertEquals(1, step.getFieldChanges().size());

        FieldChangeResponse behaviourChange = findField(step, "expectedBehaviour");
        assertEquals("behaviour v1", behaviourChange.getOldValue());
        assertEquals("behaviour v2", behaviourChange.getNewValue());
        assertEquals(ChangeType.CHANGED, behaviourChange.getChangeType());
    }

    // ---------- 3. multiple changed fields in the same step ----------

    @Test
    void multipleChangedFieldsInTheSameStepAreAllReported() {
        TestProcedureResponse created = createProcedure(
                "AI-182 Multi Field Change",
                List.of(buildStepRequest(null, "behaviour v1", "criteria v1", 1)));
        Long stepId = created.getSteps().get(0).getId();

        testProcedureService.update(created.getId(), buildProcedureRequest(
                "AI-182 Multi Field Change",
                List.of(buildStepRequest(stepId, "behaviour v2", "criteria v2", 1))));

        VersionComparisonResponse comparison = versionComparisonService.compare(created.getId(), 1, 2);

        StepComparisonResponse step = comparison.getSteps().get(0);
        assertEquals(ChangeType.CHANGED, step.getChangeType());
        assertEquals(2, step.getFieldChanges().size());
        assertEquals("behaviour v2", findField(step, "expectedBehaviour").getNewValue());
        assertEquals("criteria v2", findField(step, "acceptanceCriteria").getNewValue());
    }

    // ---------- 4. multiple changed steps ----------

    @Test
    void multipleChangedStepsAreAllReported() {
        TestProcedureResponse created = createProcedure(
                "AI-182 Multi Step Change",
                List.of(
                        buildStepRequest("step1 v1", 1),
                        buildStepRequest("step2 v1", 2)));
        Long step1Id = created.getSteps().get(0).getId();
        Long step2Id = created.getSteps().get(1).getId();

        testProcedureService.update(created.getId(), buildProcedureRequest(
                "AI-182 Multi Step Change",
                List.of(
                        buildStepRequest(step1Id, "step1 v2", "criteria", 1),
                        buildStepRequest(step2Id, "step2 v2", "criteria", 2))));

        VersionComparisonResponse comparison = versionComparisonService.compare(created.getId(), 1, 2);

        assertEquals(2, comparison.getSteps().size());
        assertEquals(2, comparison.getChangedStepCount());
        assertTrue(comparison.getSteps().stream().allMatch(s -> s.getChangeType() == ChangeType.CHANGED));
    }

    // ---------- 5. added step ----------

    @Test
    void anAddedStepIsReportedAsAdded() {
        TestProcedureResponse created = createProcedure(
                "AI-182 Added Step",
                List.of(buildStepRequest("step1 v1", 1)));
        Long step1Id = created.getSteps().get(0).getId();

        testProcedureService.update(created.getId(), buildProcedureRequest(
                "AI-182 Added Step",
                List.of(
                        buildStepRequest(step1Id, "step1 v1", "criteria", 1),
                        buildStepRequest(null, "step2 new", "criteria", 2))));

        VersionComparisonResponse comparison = versionComparisonService.compare(created.getId(), 1, 2);

        assertEquals(2, comparison.getSteps().size());
        assertEquals(1, comparison.getAddedStepCount());
        assertEquals(1, comparison.getUnchangedStepCount());

        StepComparisonResponse addedStep = comparison.getSteps().stream()
                .filter(s -> s.getChangeType() == ChangeType.ADDED)
                .findFirst().orElseThrow();
        assertNull(addedStep.getFromStep());
        assertNotNull(addedStep.getToStep());
        assertEquals("step2 new", addedStep.getToStep().getExpectedBehaviour());
        assertTrue(addedStep.getFieldChanges().isEmpty(), "Added/removed steps carry no per-field diff entries");
    }

    // ---------- 6. removed step ----------

    @Test
    void aRemovedStepIsReportedAsRemoved() {
        TestProcedureResponse created = createProcedure(
                "AI-182 Removed Step",
                List.of(
                        buildStepRequest("step1 v1", 1),
                        buildStepRequest("step2 v1", 2)));
        Long step1Id = created.getSteps().get(0).getId();

        testProcedureService.update(created.getId(), buildProcedureRequest(
                "AI-182 Removed Step",
                List.of(buildStepRequest(step1Id, "step1 v1", "criteria", 1))));

        VersionComparisonResponse comparison = versionComparisonService.compare(created.getId(), 1, 2);

        assertEquals(2, comparison.getSteps().size());
        assertEquals(1, comparison.getRemovedStepCount());

        StepComparisonResponse removedStep = comparison.getSteps().stream()
                .filter(s -> s.getChangeType() == ChangeType.REMOVED)
                .findFirst().orElseThrow();
        assertNotNull(removedStep.getFromStep());
        assertNull(removedStep.getToStep());
        assertEquals("step2 v1", removedStep.getFromStep().getExpectedBehaviour());
    }

    // ---------- 7. execution order changes / reordering ----------

    @Test
    void reorderingStepsIsReportedAsAnExecutionOrderFieldChangeNotAddRemove() {
        TestProcedureResponse created = createProcedure(
                "AI-182 Reorder",
                List.of(
                        buildStepRequest("step A", 1),
                        buildStepRequest("step B", 2)));
        Long stepAId = created.getSteps().get(0).getId();
        Long stepBId = created.getSteps().get(1).getId();

        // Swap positions: B first, then A.
        testProcedureService.update(created.getId(), buildProcedureRequest(
                "AI-182 Reorder",
                List.of(
                        buildStepRequest(stepBId, "step B", "criteria", 1),
                        buildStepRequest(stepAId, "step A", "criteria", 2))));

        VersionComparisonResponse comparison = versionComparisonService.compare(created.getId(), 1, 2);

        assertEquals(0, comparison.getAddedStepCount(), "Reordering must not be reported as add/remove");
        assertEquals(0, comparison.getRemovedStepCount(), "Reordering must not be reported as add/remove");
        assertEquals(2, comparison.getChangedStepCount(), "Both steps' executionOrder changed");

        StepComparisonResponse stepAComparison = comparison.getSteps().stream()
                .filter(s -> stepAId.equals(s.getToStep() != null ? s.getToStep().getId() : null))
                .findFirst().orElseThrow();
        FieldChangeResponse orderChange = findField(stepAComparison, "executionOrder");
        assertEquals("1", orderChange.getOldValue());
        assertEquals("2", orderChange.getNewValue());
    }

    // ---------- 8. revision not found ----------

    @Test
    void comparingAgainstANonExistentRevisionThrowsNoSuchElement() {
        TestProcedureResponse created = createProcedure(
                "AI-182 Missing Revision",
                List.of(buildStepRequest("step1", 1)));

        assertThrows(NoSuchElementException.class,
                () -> versionComparisonService.compare(created.getId(), 1, 99));
    }

    // ---------- 9. invalid / non-existent procedure id ----------

    @Test
    void comparingAgainstANonExistentProcedureThrowsNoSuchElement() {
        long bogusProcedureId = findUnusedProcedureId();

        assertThrows(NoSuchElementException.class,
                () -> versionComparisonService.compare(bogusProcedureId, 1, 2));
    }

    @Test
    void nullOrNonPositiveVersionNumbersAreRejectedAsInvalid() {
        TestProcedureResponse created = createProcedure(
                "AI-182 Invalid Version Params",
                List.of(buildStepRequest("step1", 1)));

        assertThrows(IllegalArgumentException.class,
                () -> versionComparisonService.compare(created.getId(), null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> versionComparisonService.compare(created.getId(), 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> versionComparisonService.compare(created.getId(), -1, 1));
    }

    // ---------- 10. null / empty optional field changes ----------

    @Test
    void blankOptionalFieldIsTreatedTheSameAsNullNotAFalsePositiveChange() {
        TestStepRequest step = buildStepRequest(null, "behaviour", "criteria", 1);
        step.setExpectedRange(null);
        TestProcedureResponse created = createProcedure("AI-182 Null Optional Field", List.of(step));
        Long stepId = created.getSteps().get(0).getId();

        TestStepRequest updatedStep = buildStepRequest(stepId, "behaviour", "criteria", 1);
        updatedStep.setExpectedRange(""); // blank, not null - should NOT register as a change
        testProcedureService.update(created.getId(), buildProcedureRequest(
                "AI-182 Null Optional Field", List.of(updatedStep)));

        VersionComparisonResponse comparison = versionComparisonService.compare(created.getId(), 1, 2);
        StepComparisonResponse step1 = comparison.getSteps().get(0);
        assertEquals(ChangeType.UNCHANGED, step1.getChangeType(),
                "null -> blank on an optional field should not be reported as a change");

        // Now give it a real value: this SHOULD register as an ADDED field change.
        TestStepRequest withValue = buildStepRequest(stepId, "behaviour", "criteria", 1);
        withValue.setExpectedRange("0-5V");
        testProcedureService.update(created.getId(), buildProcedureRequest(
                "AI-182 Null Optional Field", List.of(withValue)));

        VersionComparisonResponse secondComparison = versionComparisonService.compare(created.getId(), 1, 3);
        StepComparisonResponse step1Again = secondComparison.getSteps().get(0);
        FieldChangeResponse rangeChange = findField(step1Again, "expectedRange");
        assertEquals(ChangeType.ADDED, rangeChange.getChangeType());
        assertNull(rangeChange.getOldValue());
        assertEquals("0-5V", rangeChange.getNewValue());
    }

    // ---------- 11. historical snapshots used, not current mutable rows ----------

    @Test
    void comparisonUsesHistoricalSnapshotsEvenAfterTheLiveStepsHaveMovedOnFurther() {
        TestProcedureResponse created = createProcedure(
                "AI-182 Snapshot Not Live Rows",
                List.of(buildStepRequest("behaviour v1", 1)));
        Long stepId = created.getSteps().get(0).getId();

        testProcedureService.update(created.getId(), buildProcedureRequest(
                "AI-182 Snapshot Not Live Rows",
                List.of(buildStepRequest(stepId, "behaviour v2", "criteria", 1))));

        // A third save moves the LIVE row on to "behaviour v3", past both compared revisions.
        testProcedureService.update(created.getId(), buildProcedureRequest(
                "AI-182 Snapshot Not Live Rows",
                List.of(buildStepRequest(stepId, "behaviour v3", "criteria", 1))));

        // Comparing revision 1 -> 2 must still show v1 -> v2, not be polluted by the live v3 row.
        VersionComparisonResponse comparison = versionComparisonService.compare(created.getId(), 1, 2);
        StepComparisonResponse step = comparison.getSteps().get(0);
        FieldChangeResponse behaviourChange = findField(step, "expectedBehaviour");
        assertEquals("behaviour v1", behaviourChange.getOldValue());
        assertEquals("behaviour v2", behaviourChange.getNewValue());

        TestStep liveRow = testStepRepository.findById(stepId).orElseThrow();
        assertEquals("behaviour v3", liveRow.getExpectedBehaviour(),
                "sanity check: the live mutable row has moved on to v3");
    }

    private long findUnusedProcedureId() {
        Optional<TestProcedure> maxProcedure = testProcedureRepository.findAll().stream()
                .max((a, b) -> Long.compare(a.getId(), b.getId()));
        long base = maxProcedure.map(TestProcedure::getId).orElse(0L);
        return base + 1_000_000L;
    }
}
