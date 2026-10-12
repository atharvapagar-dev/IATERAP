package com.adtech.iaterap.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.adtech.iaterap.backend.dto.SyntheticEventGenerationResponse;
import com.adtech.iaterap.backend.dto.TestProcedureRequest;
import com.adtech.iaterap.backend.dto.TestProcedureResponse;
import com.adtech.iaterap.backend.dto.TestStepRequest;
import com.adtech.iaterap.backend.entity.Connector;
import com.adtech.iaterap.backend.entity.FaultTemplate;
import com.adtech.iaterap.backend.entity.Interface;
import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.ProcedureHistory;
import com.adtech.iaterap.backend.entity.ProtocolTag;
import com.adtech.iaterap.backend.entity.SignalLibrary;
import com.adtech.iaterap.backend.entity.TestStep;
import com.adtech.iaterap.backend.entity.ValidationReading;
import com.adtech.iaterap.backend.entity.ValidationRun;
import com.adtech.iaterap.backend.repository.ConnectorRepository;
import com.adtech.iaterap.backend.repository.FaultTemplateRepository;
import com.adtech.iaterap.backend.repository.InterfaceRepository;
import com.adtech.iaterap.backend.repository.LruRepository;
import com.adtech.iaterap.backend.repository.ProcedureHistoryRepository;
import com.adtech.iaterap.backend.repository.ProtocolTagRepository;
import com.adtech.iaterap.backend.repository.SignalLibraryRepository;
import com.adtech.iaterap.backend.repository.TestProcedureRepository;
import com.adtech.iaterap.backend.repository.TestStepRepository;
import com.adtech.iaterap.backend.repository.ValidationReadingRepository;
import com.adtech.iaterap.backend.repository.ValidationRunRepository;
import com.adtech.iaterap.backend.validation.ValidationResult;
import com.adtech.iaterap.backend.validation.ValidationRunStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Real database proof that a persisted missing reading creates idempotent fleet evidence. */
@SpringBootTest
@ActiveProfiles("synthetic-data")
@Transactional
class SyntheticEventGenerationIntegrationTest {

    @Autowired private SyntheticEventGenerationService service;
    @Autowired private TestProcedureService testProcedureService;
    @Autowired private ValidationRunRepository runs;
    @Autowired private ValidationReadingRepository readings;
    @Autowired private ProcedureHistoryRepository histories;
    @Autowired private TestProcedureRepository procedures;
    @Autowired private TestStepRepository steps;
    @Autowired private LruRepository lrus;
    @Autowired private InterfaceRepository interfaces;
    @Autowired private ConnectorRepository connectors;
    @Autowired private SignalLibraryRepository signals;
    @Autowired private ProtocolTagRepository protocols;
    @Autowired private FaultTemplateRepository faults;
    @Autowired private JdbcTemplate jdbc;

    private Long runId;
    private Long procedureId;

    @AfterEach
    void cleanUp() {
        if (runId != null) {
            jdbc.update("DELETE FROM event_log WHERE source_validation_run_id = ?", runId);
            readings.deleteAll(readings.findByValidationRunIdOrderBySourceRowNumberAsc(runId));
            runs.deleteById(runId);
        }

        if (procedureId != null) {
            List<ProcedureHistory> procedureHistories =
                    histories.findByTestProcedureIdOrderByVersionNumberAsc(procedureId);
            histories.deleteAll(procedureHistories);

            List<TestStep> procedureSteps =
                    steps.findByTestProcedureIdOrderByExecutionOrderAsc(procedureId);
            steps.deleteAll(procedureSteps);

            procedures.deleteById(procedureId);
        }
    }

    @Test
    void turnsPersistedMissingReadingIntoTraceableEventsWithoutDuplicates() {
        Lru lru = lrus.findAll().stream().findFirst()
                .orElseThrow(() -> new AssertionError("Synthetic-data profile did not seed an LRU"));

        Interface iface = interfaces.findAll().stream()
                .filter(candidate -> candidate.getLru().getId().equals(lru.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No interface found for seeded LRU " + lru.getId()));

        Connector connector = connectors.findByLruId(lru.getId()).stream().findFirst()
                .orElseThrow(() -> new AssertionError("No connector found for seeded LRU " + lru.getId()));
        SignalLibrary signal = signals.findByLruId(lru.getId()).stream().findFirst()
                .orElseThrow(() -> new AssertionError("No signal found for seeded LRU " + lru.getId()));
        ProtocolTag protocol = protocols.findByLruId(lru.getId()).stream().findFirst()
                .orElseThrow(() -> new AssertionError("No protocol tag found for seeded LRU " + lru.getId()));
        FaultTemplate fault = faults.findByLruId(lru.getId()).stream().findFirst()
                .orElseThrow(() -> new AssertionError("No fault template found for seeded LRU " + lru.getId()));

        TestStepRequest stepRequest = new TestStepRequest();
        stepRequest.setInterfaceId(iface.getId());
        stepRequest.setConnectorId(connector.getId());
        stepRequest.setSignalLibraryId(signal.getId());
        stepRequest.setProtocolTagId(protocol.getId());
        stepRequest.setFaultCategoryId(fault.getId());
        stepRequest.setExpectedBehaviour("Synthetic integration test expected behaviour");
        stepRequest.setAcceptanceCriteria("The synthetic test step is recorded");
        stepRequest.setPassFailCriteria("Pass when the test step is present");
        stepRequest.setExecutionOrder(1);

        TestProcedureRequest procedureRequest = new TestProcedureRequest();
        procedureRequest.setLruId(lru.getId());
        procedureRequest.setName("AI-245 synthetic event integration " + UUID.randomUUID());
        procedureRequest.setDescription("Test-owned fixture for synthetic event generation");
        procedureRequest.setSteps(List.of(stepRequest));

        TestProcedureResponse procedure = testProcedureService.save(procedureRequest);
        procedureId = procedure.getId();
        assertNotNull(procedureId, "Procedure service must create the test procedure");

        ProcedureHistory history = histories
                .findTopByTestProcedureIdOrderByVersionNumberDesc(procedureId)
                .orElseThrow(() -> new AssertionError("Procedure service did not create revision history"));

        TestStep createdStep = steps.findByTestProcedureIdOrderByExecutionOrderAsc(procedureId)
                .stream().findFirst()
                .orElseThrow(() -> new AssertionError("Procedure service did not create a test step"));

        Long aircraftId = jdbc.query(
                "SELECT id FROM aircraft WHERE name LIKE 'SYN-AC-%' ORDER BY id LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null);

        assertNotNull(aircraftId, "Synthetic-data profile must seed a synthetic aircraft");

        ValidationRun run = new ValidationRun();
        run.setProcedureHistoryId(history.getId());
        run.setSourceFilename("synthetic-event-generation-test.json");
        run.setSourceSha256(
                UUID.randomUUID().toString().replace("-", "")
                        + UUID.randomUUID().toString().replace("-", ""));
        run.setSourceFormat("JSON");
        run.setRecordedBy("AI245_TEST");
        run.setStatus(ValidationRunStatus.VALIDATED);
        run.setUploadedAt(LocalDateTime.now());
        run.setTotalReadings(1);
        run.setMatchedCount(0);
        run.setMismatchedCount(0);
        run.setOutlierCount(0);
        run.setMissingCount(1);
        run = runs.saveAndFlush(run);
        runId = run.getId();

        ValidationReading reading = new ValidationReading();
        reading.setValidationRunId(runId);
        reading.setTestStepId(createdStep.getId());
        reading.setResult(ValidationResult.MISSING);
        readings.saveAndFlush(reading);

        SyntheticEventGenerationResponse first = service.generate(aircraftId);

        assertEquals(3, jdbc.queryForObject(
                "SELECT COUNT(*) FROM event_log WHERE source_validation_run_id = ?",
                Integer.class, runId));
        assertEquals(lru.getId(), jdbc.queryForObject(
                "SELECT lru_id FROM event_log WHERE source_validation_run_id = ? LIMIT 1",
                Long.class, runId));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(DISTINCT synthetic_lru_instance_id) FROM event_log WHERE source_validation_run_id = ?",
                Integer.class, runId));

        SyntheticEventGenerationResponse second = service.generate(aircraftId);

        assertEquals(3, jdbc.queryForObject(
                "SELECT COUNT(*) FROM event_log WHERE source_validation_run_id = ?",
                Integer.class, runId));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM event_log WHERE source_validation_run_id = ? AND synthetic_lru_instance_id IS NULL",
                Integer.class, runId));

        // The responses include other runs in the database, so assert idempotency
        // against the database rows scoped to this fixture's run ID.
        assertNotNull(first);
        assertNotNull(second);
    }
}
