package com.adtech.iaterap.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adtech.iaterap.backend.dto.SyntheticEventGenerationResponse;
import com.adtech.iaterap.backend.entity.Aircraft;
import com.adtech.iaterap.backend.entity.ProcedureHistory;
import com.adtech.iaterap.backend.entity.SyntheticLruInstance;
import com.adtech.iaterap.backend.entity.ValidationReading;
import com.adtech.iaterap.backend.entity.ValidationRun;
import com.adtech.iaterap.backend.repository.AircraftRepository;
import com.adtech.iaterap.backend.repository.EventLogRepository;
import com.adtech.iaterap.backend.repository.ProcedureHistoryRepository;
import com.adtech.iaterap.backend.repository.SyntheticLruInstanceRepository;
import com.adtech.iaterap.backend.repository.ValidationReadingRepository;
import com.adtech.iaterap.backend.repository.ValidationRunRepository;
import com.adtech.iaterap.backend.validation.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SyntheticEventGenerationServiceTest {

    @Test
    void generatesTraceableSortieFailureAndRepairEventsFromValidationFindings() {
        Fixture fixture = new Fixture(false, false);

        SyntheticEventGenerationResponse response = fixture.service.generate(3L);

        assertEquals(3L, response.aircraftId());
        assertEquals(1, response.validationRunsConsidered());
        assertEquals(1, response.syntheticInstancesCreated());
        assertEquals(3, response.eventsCreated());
        assertEquals(0, response.eventsAlreadyPresent());
        verify(fixture.events, org.mockito.Mockito.times(3)).save(any());
    }

    @Test
    void regenerationIsIdempotentAndDoesNotDuplicateExistingEvidence() {
        Fixture fixture = new Fixture(true, true);

        SyntheticEventGenerationResponse response = fixture.service.generate(3L);

        assertEquals(0, response.syntheticInstancesCreated());
        assertEquals(0, response.eventsCreated());
        assertEquals(3, response.eventsAlreadyPresent());
        verify(fixture.events, org.mockito.Mockito.never()).save(any());
    }

    private static final class Fixture {
        private final EventLogRepository events = mock(EventLogRepository.class);
        private final SyntheticEventGenerationService service;

        private Fixture(boolean hasInstance, boolean hasEvents) {
            AircraftRepository aircrafts = mock(AircraftRepository.class);
            ValidationRunRepository runs = mock(ValidationRunRepository.class);
            ValidationReadingRepository readings = mock(ValidationReadingRepository.class);
            ProcedureHistoryRepository histories = mock(ProcedureHistoryRepository.class);
            SyntheticLruInstanceRepository instances = mock(SyntheticLruInstanceRepository.class);

            Aircraft aircraft = mock(Aircraft.class);
            when(aircraft.getId()).thenReturn(3L);
            when(aircraft.getName()).thenReturn("Demo Aircraft");
            when(aircrafts.findById(3L)).thenReturn(Optional.of(aircraft));

            ValidationRun run = mock(ValidationRun.class);
            when(run.getId()).thenReturn(44L);
            when(run.getProcedureHistoryId()).thenReturn(77L);
            when(run.getUploadedAt()).thenReturn(LocalDateTime.of(2026, 10, 11, 10, 0));
            when(runs.findAll()).thenReturn(List.of(run));

            ProcedureHistory history = mock(ProcedureHistory.class);
            when(history.getId()).thenReturn(77L);
            when(history.getLruId()).thenReturn(12L);
            when(histories.findById(77L)).thenReturn(Optional.of(history));

            SyntheticLruInstance instance = mock(SyntheticLruInstance.class);
            when(instance.getId()).thenReturn(91L);
            when(instance.getLruId()).thenReturn(12L);
            when(instances.findByAircraftIdAndLruIdAndInstanceCode(3L, 12L, "SYN-AC3-LRU12"))
                    .thenReturn(hasInstance ? Optional.of(instance) : Optional.empty());
            when(instances.save(any(SyntheticLruInstance.class))).thenReturn(instance);

            ValidationReading match = mock(ValidationReading.class);
            when(match.getResult()).thenReturn(ValidationResult.MATCH);
            ValidationReading mismatch = mock(ValidationReading.class);
            when(mismatch.getResult()).thenReturn(ValidationResult.MISMATCH);
            when(readings.findByValidationRunIdOrderBySourceRowNumberAsc(44L)).thenReturn(List.of(match, mismatch));
            when(events.existsBySyntheticLruInstanceIdAndSourceValidationRunIdAndEventType(eq(91L), eq(44L), any()))
                    .thenReturn(hasEvents);

            service = new SyntheticEventGenerationService(aircrafts, runs, readings, histories,
                    instances, events, new ObjectMapper());
        }
    }
}
