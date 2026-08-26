package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.ChangeType;
import com.adtech.iaterap.backend.dto.ProcedureHistoryResponse;
import com.adtech.iaterap.backend.dto.TestProcedureRequest;
import com.adtech.iaterap.backend.dto.TestProcedureResponse;
import com.adtech.iaterap.backend.dto.TestStepRequest;
import com.adtech.iaterap.backend.dto.VersionComparisonResponse;
import com.adtech.iaterap.backend.entity.AuditLog;
import com.adtech.iaterap.backend.entity.Connector;
import com.adtech.iaterap.backend.entity.FaultTemplate;
import com.adtech.iaterap.backend.entity.Interface;
import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.ProtocolTag;
import com.adtech.iaterap.backend.entity.SignalLibrary;
import com.adtech.iaterap.backend.repository.AuditLogRepository;
import com.adtech.iaterap.backend.repository.ConnectorRepository;
import com.adtech.iaterap.backend.repository.FaultTemplateRepository;
import com.adtech.iaterap.backend.repository.InterfaceRepository;
import com.adtech.iaterap.backend.repository.LruRepository;
import com.adtech.iaterap.backend.repository.ProcedureHistoryRepository;
import com.adtech.iaterap.backend.repository.ProtocolTagRepository;
import com.adtech.iaterap.backend.repository.SignalLibraryRepository;
import com.adtech.iaterap.backend.repository.TestProcedureRepository;
import com.adtech.iaterap.backend.repository.TestStepRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI-187 / T7-B3: end-to-end rollback tests.
 *
 * <p>Follows the same integration-style convention as
 * {@link VersionComparisonServiceTest}: real repositories against the
 * synthetic-data test database, driving revisions through the actual
 * {@link TestProcedureService} save/update/rollback flow rather than
 * hand-inserting {@code procedure_history} rows, so these tests exercise
 * the exact non-destructive rollback behavior end to end -- including its
 * integration with {@link VersionComparisonService} (Sec 11 of the T7-B3
 * requirements: {@code compare(v1, v4)} must show no field-level content
 * differences after a rollback from v3 to v1).</p>
 */
@SpringBootTest
@ActiveProfiles("synthetic-data")
class ProcedureRollbackIntegrationTest {

    @Autowired private TestProcedureService testProcedureService;
    @Autowired private VersionComparisonService versionComparisonService;
    @Autowired private TestProcedureRepository testProcedureRepository;
    @Autowired private TestStepRepository testStepRepository;
    @Autowired private ProcedureHistoryRepository procedureHistoryRepository;
    @Autowired private AuditLogRepository auditLogRepository;
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
            jdbcTemplate.update("DELETE FROM audit_log WHERE entity_name = 'TestProcedure' AND entity_id = ?",
                    String.valueOf(procedureId));
            jdbcTemplate.update("DELETE FROM procedure_history WHERE test_procedure_id = ?", procedureId);
            testStepRepository.deleteAll(testStepRepository.findByTestProcedureIdOrderByExecutionOrderAsc(procedureId));
            testProcedureRepository.deleteById(procedureId);
        }
        createdProcedureIds.clear();
    }

    // ---------- fixtures (mirrors VersionComparisonServiceTest) ----------

    private TestStepRequest buildStepRequest(Long id, String behaviour, String acceptanceCriteria, int order) {
        Lru lru = lruRepository.findAll().get(0);
        Interface iface = interfaceRepository.findAll().stream()
                .filter(i -> i.getLru().getId().equals(lru.getId()))
                .findFirst()
                .orElseThrow();
        Connector connector = connectorRepository.findByLruId(lru.getId()).get(0);
        SignalLibrary signal = signalLibraryRepository.findByLruId(lru.getId()).get(0);
        ProtocolTag protocolTag = protocolTagRepository.findByLruId(lru.getId()).get(0);
        FaultTemplate fault = faultTemplateRepository.findByLruId(lru.getId()).get(0);

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

    private TestProcedureRequest buildProcedureRequest(String name, List<TestStepRequest> steps) {
        Lru lru = lruRepository.findAll().get(0);
        TestProcedureRequest request = new TestProcedureRequest();
        request.setLruId(lru.getId());
        request.setName(name);
        request.setDescription("AI-187 rollback fixture");
        request.setSteps(steps);
        return request;
    }

    private TestProcedureResponse createProcedure(String name, List<TestStepRequest> steps) {
        TestProcedureResponse created = testProcedureService.save(buildProcedureRequest(name, steps));
        createdProcedureIds.add(created.getId());
        return created;
    }

    // ---------- 1. core non-destructive rollback flow ----------

    @Test
    void rollbackCreatesANewRevisionAndLeavesAllPriorRevisionsUntouched() {
        TestProcedureResponse created = createProcedure(
                "AI-187 Core Flow",
                List.of(buildStepRequest(null, "behaviour v1", "criteria v1", 1)));
        Long procedureId = created.getId();
        Long stepId = created.getSteps().get(0).getId();

        testProcedureService.update(procedureId, buildProcedureRequest(
                "AI-187 Core Flow v2",
                List.of(buildStepRequest(stepId, "behaviour v2", "criteria v2", 1))));

        testProcedureService.update(procedureId, buildProcedureRequest(
                "AI-187 Core Flow v3",
                List.of(buildStepRequest(stepId, "behaviour v3", "criteria v3", 1))));

        ProcedureHistoryResponse v1Before = testProcedureService.getRevision(procedureId, 1);
        ProcedureHistoryResponse v2Before = testProcedureService.getRevision(procedureId, 2);
        ProcedureHistoryResponse v3Before = testProcedureService.getRevision(procedureId, 3);

        TestProcedureResponse rollbackResult = testProcedureService.rollbackToRevision(procedureId, 1);

        // A new revision (v4) was created -- v1's version number was never reused.
        assertEquals(4, rollbackResult.getVersionNumber());
        assertEquals("AI-187 Core Flow", rollbackResult.getName());
        assertEquals(1, rollbackResult.getSteps().size());
        assertEquals("behaviour v1", rollbackResult.getSteps().get(0).getExpectedBehaviour());

        // v1, v2, and v3 are all byte-for-byte unchanged.
        ProcedureHistoryResponse v1After = testProcedureService.getRevision(procedureId, 1);
        ProcedureHistoryResponse v2After = testProcedureService.getRevision(procedureId, 2);
        ProcedureHistoryResponse v3After = testProcedureService.getRevision(procedureId, 3);
        assertEquals(v1Before.getStepsSnapshot(), v1After.getStepsSnapshot());
        assertEquals(v2Before.getStepsSnapshot(), v2After.getStepsSnapshot());
        assertEquals(v3Before.getStepsSnapshot(), v3After.getStepsSnapshot());
        assertEquals(v1Before.getName(), v1After.getName());

        // v4 exists as its own, separate, immutable revision.
        ProcedureHistoryResponse v4 = testProcedureService.getRevision(procedureId, 4);
        assertEquals(4, v4.getVersionNumber());
        assertTrue(v4.getStepsSnapshot().contains("behaviour v1"));
    }

    // ---------- 2. Version Comparison integration (Sec 11) ----------

    @Test
    void comparingV1AndTheRollbackRevisionShowsNoFieldLevelDifferences() {
        TestProcedureResponse created = createProcedure(
                "AI-187 Compare v1 v4",
                List.of(buildStepRequest(null, "behaviour v1", "criteria v1", 1)));
        Long procedureId = created.getId();
        Long stepId = created.getSteps().get(0).getId();

        testProcedureService.update(procedureId, buildProcedureRequest(
                "AI-187 Compare v1 v4 (v2 name)",
                List.of(buildStepRequest(stepId, "behaviour v2", "criteria v2", 1))));

        testProcedureService.update(procedureId, buildProcedureRequest(
                "AI-187 Compare v1 v4 (v3 name)",
                List.of(buildStepRequest(stepId, "behaviour v3", "criteria v3", 1))));

        testProcedureService.rollbackToRevision(procedureId, 1);

        VersionComparisonResponse v1VsV4 = versionComparisonService.compare(procedureId, 1, 4);

        assertEquals(1, v1VsV4.getSteps().size());
        assertEquals(ChangeType.UNCHANGED, v1VsV4.getSteps().get(0).getChangeType(),
                "The same live step id was reused by rollback, so v1 and v4 must match by id "
                        + "with identical field values.");
        assertTrue(v1VsV4.getSteps().get(0).getFieldChanges().isEmpty());
        assertEquals(0, v1VsV4.getChangedStepCount());
        assertEquals(1, v1VsV4.getUnchangedStepCount());
    }

    @Test
    void comparingTheSourceRevisionAndTheRollbackRevisionShowsTheExpectedDifferences() {
        TestProcedureResponse created = createProcedure(
                "AI-187 Compare v3 v4",
                List.of(buildStepRequest(null, "behaviour v1", "criteria v1", 1)));
        Long procedureId = created.getId();
        Long stepId = created.getSteps().get(0).getId();

        testProcedureService.update(procedureId, buildProcedureRequest(
                "AI-187 Compare v3 v4",
                List.of(buildStepRequest(stepId, "behaviour v2", "criteria v2", 1))));

        testProcedureService.update(procedureId, buildProcedureRequest(
                "AI-187 Compare v3 v4",
                List.of(buildStepRequest(stepId, "behaviour v3", "criteria v3", 1))));

        testProcedureService.rollbackToRevision(procedureId, 1);

        VersionComparisonResponse v3VsV4 = versionComparisonService.compare(procedureId, 3, 4);

        assertEquals(ChangeType.CHANGED, v3VsV4.getSteps().get(0).getChangeType());
        assertEquals(1, v3VsV4.getChangedStepCount());
        boolean behaviourReverted = v3VsV4.getSteps().get(0).getFieldChanges().stream()
                .anyMatch(f -> f.getFieldName().equals("expectedBehaviour")
                        && f.getOldValue().equals("behaviour v3")
                        && f.getNewValue().equals("behaviour v1"));
        assertTrue(behaviourReverted, "compare(v3, v4) should show the field reverting from v3's value back to v1's");
    }

    // ---------- 3. audit (Sec 7) ----------

    @Test
    void rollbackWritesARollbackAuditLogEntry() {
        TestProcedureResponse created = createProcedure(
                "AI-187 Audit",
                List.of(buildStepRequest(null, "behaviour v1", "criteria v1", 1)));
        Long procedureId = created.getId();
        Long stepId = created.getSteps().get(0).getId();

        testProcedureService.update(procedureId, buildProcedureRequest(
                "AI-187 Audit v2",
                List.of(buildStepRequest(stepId, "behaviour v2", "criteria v2", 1))));

        testProcedureService.rollbackToRevision(procedureId, 1);

        List<AuditLog> logs = auditLogRepository.findByEntityNameAndEntityIdOrderByCreatedAtDesc(
                "TestProcedure", String.valueOf(procedureId));

        assertTrue(logs.stream().anyMatch(l -> "ROLLBACK".equals(l.getAction())),
                "Expected a ROLLBACK audit entry for procedure " + procedureId);
    }

    // ---------- 4. error handling (Sec 12) ----------

    @Test
    void rollbackToAMissingRevisionReturns404Equivalent() {
        TestProcedureResponse created = createProcedure(
                "AI-187 Missing Revision",
                List.of(buildStepRequest(null, "behaviour v1", "criteria v1", 1)));

        assertThrows(NoSuchElementException.class,
                () -> testProcedureService.rollbackToRevision(created.getId(), 99));
    }

    @Test
    void rollbackOnAMissingProcedureReturns404Equivalent() {
        assertThrows(NoSuchElementException.class,
                () -> testProcedureService.rollbackToRevision(-1L, 1));
    }

    @Test
    void rollbackUsingARevisionThatBelongsToAnotherProcedureFails() {
        TestProcedureResponse procedureA = createProcedure(
                "AI-187 Procedure A",
                List.of(buildStepRequest(null, "A behaviour v1", "criteria", 1)));
        TestProcedureResponse procedureB = createProcedure(
                "AI-187 Procedure B",
                List.of(buildStepRequest(null, "B behaviour v1", "criteria", 1)));

        // procedureB has no revision 2 of its own -- and revision 2 of
        // procedureA must not be reachable through procedureB's id.
        testProcedureService.update(procedureA.getId(), buildProcedureRequest(
                "AI-187 Procedure A v2",
                List.of(buildStepRequest(procedureA.getSteps().get(0).getId(), "A behaviour v2", "criteria", 1))));

        assertThrows(NoSuchElementException.class,
                () -> testProcedureService.rollbackToRevision(procedureB.getId(), 2));
    }

    @Test
    void rollbackRejectsAnInvalidRevisionNumber() {
        TestProcedureResponse created = createProcedure(
                "AI-187 Invalid Revision",
                List.of(buildStepRequest(null, "behaviour v1", "criteria v1", 1)));

        assertThrows(IllegalArgumentException.class,
                () -> testProcedureService.rollbackToRevision(created.getId(), 0));
    }

    // ---------- 5. always creates a new revision (Sec 6 / Sec 9) ----------

    @Test
    void rollingBackToTheCurrentRevisionStillCreatesANewRevision() {
        TestProcedureResponse created = createProcedure(
                "AI-187 Rollback To Current",
                List.of(buildStepRequest(null, "behaviour v1", "criteria v1", 1)));
        Long procedureId = created.getId();

        // Rolling back v1 (the only, and therefore current, revision) to
        // itself must still append a brand new revision -- rollback is an
        // explicit action and is never suppressed as a no-op resave.
        TestProcedureResponse rollbackResult = testProcedureService.rollbackToRevision(procedureId, 1);

        assertEquals(2, rollbackResult.getVersionNumber());
        assertNotEquals(1, rollbackResult.getVersionNumber());

        ProcedureHistoryResponse v2 = testProcedureService.getRevision(procedureId, 2);
        ProcedureHistoryResponse v1 = testProcedureService.getRevision(procedureId, 1);
        assertEquals(v1.getStepsSnapshot(), v2.getStepsSnapshot());
    }

    // ---------- 6. approval status is not silently changed (Sec 9) ----------

    @Test
    void rollbackDoesNotInventANewApprovalStatus() {
        TestProcedureResponse created = createProcedure(
                "AI-187 Approval Status",
                List.of(buildStepRequest(null, "behaviour v1", "criteria v1", 1)));
        Long procedureId = created.getId();
        String statusBeforeRollback = created.getApprovalStatus();

        testProcedureService.update(procedureId, buildProcedureRequest(
                "AI-187 Approval Status v2",
                List.of(buildStepRequest(created.getSteps().get(0).getId(), "behaviour v2", "criteria", 1))));

        TestProcedureResponse rollbackResult = testProcedureService.rollbackToRevision(procedureId, 1);

        // Rollback carries forward whatever approval state the live
        // procedure already had -- exactly like update() does for every
        // other content-changing save -- rather than inventing a new
        // approval transition or auto-approving the result.
        assertEquals(statusBeforeRollback, rollbackResult.getApprovalStatus());
    }
}
