package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.ReliabilityDashboardResponse;
import com.adtech.iaterap.backend.entity.ProcedureHistory;
import com.adtech.iaterap.backend.entity.ValidationReading;
import com.adtech.iaterap.backend.entity.ValidationRun;
import com.adtech.iaterap.backend.repository.ProcedureHistoryRepository;
import com.adtech.iaterap.backend.repository.ValidationReadingRepository;
import com.adtech.iaterap.backend.repository.ValidationRunRepository;
import com.adtech.iaterap.backend.validation.ValidationResult;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReliabilityAnalyticsServiceTest {

    @Test
    void groupsPersistedFindingsByLruAndLabelsEvidenceSource() throws Exception {
        ValidationRunRepository runs = mock(ValidationRunRepository.class);
        ValidationReadingRepository readings = mock(ValidationReadingRepository.class);
        ProcedureHistoryRepository histories = mock(ProcedureHistoryRepository.class);
        ValidationRun external = run(10L, 100L, "measured-run.csv", 1);
        ValidationRun synthetic = run(11L, 100L, "synthetic-fault.json", 2);
        when(runs.findAll()).thenReturn(List.of(external, synthetic));
        when(histories.findAllById(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(history(100L, 7L, "Flight Computer")));
        when(readings.findByValidationRunIdIn(List.of(10L, 11L))).thenReturn(List.of(
                reading(10L, ValidationResult.MATCH), reading(10L, ValidationResult.OUTLIER),
                reading(11L, ValidationResult.MISMATCH), reading(11L, ValidationResult.MISSING)));

        ReliabilityDashboardResponse response = new ReliabilityAnalyticsService(runs, readings, histories).dashboard();

        assertEquals(2, response.totalRuns());
        assertEquals(4, response.totalReadings());
        assertEquals(3, response.findings());
        assertEquals(75D, response.findingRate());
        var lru = response.lrus().getFirst();
        assertEquals("Flight Computer", lru.lruName());
        assertEquals(1, lru.mismatches());
        assertEquals(1, lru.outliers());
        assertEquals(1, lru.missing());
        assertEquals("INSUFFICIENT_DATA", lru.trend());
        assertEquals(List.of("EXTERNAL", "SYNTHETIC"), lru.evidenceSources());
    }

    @Test
    void marksTrendRisingOnlyWhenRecentEvidenceIsMateriallyWorse() throws Exception {
        ValidationRunRepository runs = mock(ValidationRunRepository.class);
        ValidationReadingRepository readings = mock(ValidationReadingRepository.class);
        ProcedureHistoryRepository histories = mock(ProcedureHistoryRepository.class);
        List<ValidationRun> evidence = List.of(
                run(1L, 100L, "measured-1.csv", 1), run(2L, 100L, "measured-2.csv", 2),
                run(3L, 100L, "measured-3.csv", 3), run(4L, 100L, "measured-4.csv", 4));
        when(runs.findAll()).thenReturn(evidence);
        when(histories.findAllById(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(history(100L, 7L, "Flight Computer")));
        when(readings.findByValidationRunIdIn(List.of(1L, 2L, 3L, 4L))).thenReturn(List.of(
                reading(1L, ValidationResult.MATCH), reading(2L, ValidationResult.MATCH),
                reading(3L, ValidationResult.MISMATCH), reading(4L, ValidationResult.OUTLIER)));

        var lru = new ReliabilityAnalyticsService(runs, readings, histories).dashboard().lrus().getFirst();

        assertEquals("RISING", lru.trend());
    }

    private ValidationRun run(Long id, Long historyId, String filename, int day) throws Exception {
        ValidationRun run = new ValidationRun();
        setId(run, id); run.setProcedureHistoryId(historyId); run.setSourceFilename(filename);
        run.setUploadedAt(LocalDateTime.of(2026, 10, day, 10, 0));
        return run;
    }

    private ProcedureHistory history(Long id, Long lruId, String lruName) throws Exception {
        ProcedureHistory history = new ProcedureHistory();
        setId(history, id); history.setLruId(lruId); history.setLruName(lruName);
        return history;
    }

    private ValidationReading reading(Long runId, ValidationResult result) {
        ValidationReading reading = new ValidationReading();
        reading.setValidationRunId(runId); reading.setResult(result);
        return reading;
    }

    private void setId(Object entity, Long id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id"); field.setAccessible(true); field.set(entity, id);
    }
}
