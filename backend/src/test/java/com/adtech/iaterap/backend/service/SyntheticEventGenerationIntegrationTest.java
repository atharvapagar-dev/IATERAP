package com.adtech.iaterap.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.adtech.iaterap.backend.dto.SyntheticEventGenerationResponse;
import com.adtech.iaterap.backend.entity.ValidationReading;
import com.adtech.iaterap.backend.entity.ValidationRun;
import com.adtech.iaterap.backend.repository.ValidationReadingRepository;
import com.adtech.iaterap.backend.repository.ValidationRunRepository;
import com.adtech.iaterap.backend.validation.ValidationResult;
import com.adtech.iaterap.backend.validation.ValidationRunStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Real database proof that one injected outcome becomes idempotent fleet evidence. */
@SpringBootTest
@ActiveProfiles("synthetic-data")
@Transactional
class SyntheticEventGenerationIntegrationTest {

    @Autowired private SyntheticEventGenerationService service;
    @Autowired private ValidationRunRepository runs;
    @Autowired private ValidationReadingRepository readings;
    @Autowired private JdbcTemplate jdbc;

    private Long runId;

    @AfterEach
    void cleanUp() {
        if (runId != null) {
            jdbc.update("DELETE FROM event_log WHERE source_validation_run_id = ?", runId);
            readings.deleteAll(readings.findByValidationRunIdOrderBySourceRowNumberAsc(runId));
            runs.deleteById(runId);
        }
    }

    @Test
    void turnsPersistedMismatchIntoTraceableEventsWithoutDuplicateRegeneration() {
        Long[] source = jdbc.queryForObject(
                "SELECT ph.id, ph.lru_id, ph.test_procedure_id, a.id "
                        + "FROM procedure_history ph JOIN aircraft a ON true "
                        + "WHERE EXISTS (SELECT 1 FROM test_steps ts WHERE ts.test_procedure_id = ph.test_procedure_id) "
                        + "ORDER BY ph.id, a.id LIMIT 1",
                (rs, rowNum) -> new Long[] {rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4)});
        Long testStepId = jdbc.queryForObject(
                "SELECT id FROM test_steps WHERE test_procedure_id = ? ORDER BY id LIMIT 1", Long.class, source[2]);

        ValidationRun run = new ValidationRun();
        run.setProcedureHistoryId(source[0]);
        run.setSourceFilename("synthetic-event-generation-test.json");
        run.setSourceSha256(UUID.randomUUID().toString().replace("-", ""));
        run.setSourceFormat("JSON");
        run.setRecordedBy("AI245_TEST");
        run.setStatus(ValidationRunStatus.VALIDATED);
        run.setUploadedAt(LocalDateTime.now());
        run.setTotalReadings(1);
        run.setMismatchedCount(1);
        run = runs.save(run);
        runId = run.getId();

        ValidationReading reading = new ValidationReading();
        reading.setValidationRunId(runId);
        reading.setTestStepId(testStepId);
        reading.setResult(ValidationResult.MISSING);
        readings.save(reading);

        SyntheticEventGenerationResponse first = service.generate(source[3]);
        SyntheticEventGenerationResponse second = service.generate(source[3]);

        assertTrue(first.eventsCreated() >= 3);
        assertEquals(0, second.eventsCreated());
        assertTrue(second.eventsAlreadyPresent() >= 3);
        assertEquals(3, jdbc.queryForObject(
                "SELECT COUNT(*) FROM event_log WHERE source_validation_run_id = ?", Integer.class, runId));
        assertEquals(source[1], jdbc.queryForObject(
                "SELECT lru_id FROM event_log WHERE source_validation_run_id = ? LIMIT 1", Long.class, runId));
    }
}
