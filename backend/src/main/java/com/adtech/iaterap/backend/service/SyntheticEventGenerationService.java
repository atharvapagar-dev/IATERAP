package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.SyntheticEventGenerationResponse;
import com.adtech.iaterap.backend.entity.Aircraft;
import com.adtech.iaterap.backend.entity.EventLog;
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
import com.adtech.iaterap.backend.validation.ValidationFixtureException;
import com.adtech.iaterap.backend.validation.ValidationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns persisted validation outcomes into clearly-labelled synthetic fleet
 * evidence. It never represents the result as live aircraft telemetry.
 */
@Service
public class SyntheticEventGenerationService {

    private static final String GENERATED_BY = "IATERAP_SYNTHETIC_EVENT_GENERATOR";
    private final AircraftRepository aircrafts;
    private final ValidationRunRepository runs;
    private final ValidationReadingRepository readings;
    private final ProcedureHistoryRepository histories;
    private final SyntheticLruInstanceRepository instances;
    private final EventLogRepository events;
    private final ObjectMapper objectMapper;

    public SyntheticEventGenerationService(AircraftRepository aircrafts,
                                           ValidationRunRepository runs,
                                           ValidationReadingRepository readings,
                                           ProcedureHistoryRepository histories,
                                           SyntheticLruInstanceRepository instances,
                                           EventLogRepository events,
                                           ObjectMapper objectMapper) {
        this.aircrafts = aircrafts;
        this.runs = runs;
        this.readings = readings;
        this.histories = histories;
        this.instances = instances;
        this.events = events;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SyntheticEventGenerationResponse generate(Long aircraftId) {
        Aircraft aircraft = aircrafts.findById(aircraftId)
                .orElseThrow(() -> new ValidationFixtureException("Aircraft not found: " + aircraftId));
        List<ValidationRun> validationRuns = runs.findAll();
        Map<Long, ProcedureHistory> historyById = new HashMap<>();
        int createdInstances = 0;
        int createdEvents = 0;
        int existingEvents = 0;

        for (ValidationRun run : validationRuns) {
            ProcedureHistory history = historyById.computeIfAbsent(run.getProcedureHistoryId(), id ->
                    histories.findById(id).orElse(null));
            if (history == null || history.getLruId() == null) {
                continue;
            }
            String instanceCode = "SYN-AC" + aircraft.getId() + "-LRU" + history.getLruId();
            Optional<SyntheticLruInstance> existingInstance = instances
                    .findByAircraftIdAndLruIdAndInstanceCode(aircraft.getId(), history.getLruId(), instanceCode);
            SyntheticLruInstance instance;
            if (existingInstance.isPresent()) {
                instance = existingInstance.get();
            } else {
                instance = instances.save(new SyntheticLruInstance(aircraft.getId(), history.getLruId(),
                        instanceCode, stableSeed(aircraft.getId(), history.getLruId())));
                createdInstances++;
            }

            List<ValidationReading> runReadings = readings.findByValidationRunIdOrderBySourceRowNumberAsc(run.getId());
            long findings = runReadings.stream().filter(reading -> isFinding(reading.getResult())).count();
            LocalDateTime occurredAt = run.getUploadedAt();
            EventCounts counts = new EventCounts();
            counts.add(appendIfAbsent(instance, run, "SORTIE_COMPLETED", "INFO", occurredAt,
                    payload(aircraft, history, run, runReadings.size(), findings, "Synthetic sortie evidence generated from validation.")));
            if (findings > 0) {
                counts.add(appendIfAbsent(instance, run, "FAILURE_DETECTED", "ERROR", occurredAt.plusMinutes(1),
                        payload(aircraft, history, run, runReadings.size(), findings, "Synthetic failure evidence generated from validation findings.")));
                counts.add(appendIfAbsent(instance, run, "REPAIR_COMPLETED", "WARNING", occurredAt.plusMinutes(30),
                        payload(aircraft, history, run, runReadings.size(), findings, "Synthetic repair closure following generated fault evidence.")));
            }
            createdEvents += counts.created;
            existingEvents += counts.existing;
        }
        return new SyntheticEventGenerationResponse(aircraft.getId(), validationRuns.size(),
                createdInstances, createdEvents, existingEvents);
    }

    private EventOutcome appendIfAbsent(SyntheticLruInstance instance, ValidationRun run, String type,
                                        String severity, LocalDateTime timestamp, String payload) {
        if (events.existsBySyntheticLruInstanceIdAndSourceValidationRunIdAndEventType(
                instance.getId(), run.getId(), type)) return EventOutcome.EXISTING;
        events.save(new EventLog(instance.getId(), instance.getLruId(), run.getId(), type, severity,
                timestamp, payload, GENERATED_BY));
        return EventOutcome.CREATED;
    }

    private String payload(Aircraft aircraft, ProcedureHistory history, ValidationRun run,
                           int readingsCount, long findings, String description) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "synthetic", true,
                    "description", description,
                    "aircraftId", aircraft.getId(),
                    "aircraftName", aircraft.getName(),
                    "lruId", history.getLruId(),
                    "procedureHistoryId", history.getId(),
                    "validationRunId", run.getId(),
                    "readings", readingsCount,
                    "findings", findings));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize synthetic event evidence", exception);
        }
    }

    private static boolean isFinding(ValidationResult result) {
        return result == ValidationResult.MISMATCH || result == ValidationResult.OUTLIER
                || result == ValidationResult.MISSING;
    }

    private static long stableSeed(Long aircraftId, Long lruId) {
        return aircraftId * 1_000_003L + lruId;
    }

    private enum EventOutcome { CREATED, EXISTING }
    private static final class EventCounts {
        private int created;
        private int existing;
        void add(EventOutcome outcome) { if (outcome == EventOutcome.CREATED) created++; else existing++; }
    }
}
