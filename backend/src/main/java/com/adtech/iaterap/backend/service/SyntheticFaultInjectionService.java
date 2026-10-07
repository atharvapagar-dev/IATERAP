package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.ProcedureHistory;
import com.adtech.iaterap.backend.entity.ValidationRun;
import com.adtech.iaterap.backend.repository.ProcedureHistoryRepository;
import com.adtech.iaterap.backend.validation.ExpectedRange;
import com.adtech.iaterap.backend.validation.EngineeringUnit;
import com.adtech.iaterap.backend.validation.SyntheticFaultCategory;
import com.adtech.iaterap.backend.validation.SyntheticInjectionScenario;
import com.adtech.iaterap.backend.validation.ValidationFixtureException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Generates explicitly synthetic evidence for one immutable procedure revision (T13-B1).
 * The generated evidence is passed through the same ingestion/comparison path as
 * an external CSV/JSON upload; it is never treated as a hardware measurement.
 */
@Service
public class SyntheticFaultInjectionService {

    private final ProcedureHistoryRepository historyRepository;
    private final ValidationIngestionService ingestionService;
    private final ObjectMapper objectMapper;

    public SyntheticFaultInjectionService(ProcedureHistoryRepository historyRepository,
                                          ValidationIngestionService ingestionService,
                                          ObjectMapper objectMapper) {
        this.historyRepository = historyRepository;
        this.ingestionService = ingestionService;
        this.objectMapper = objectMapper;
    }

    /**
     * Backward-compatible overload for triggering synthetic fault injection without
     * explicit category or targeting filters.
     */
    public ValidationRun run(Long procedureHistoryId, SyntheticInjectionScenario scenario,
                             String recordedBy) {
        return run(procedureHistoryId, scenario, null, null, null, recordedBy);
    }

    /**
     * Executes synthetic fault injection against an immutable approved procedure revision,
     * supporting configurable LRU targeting and multiple fault categories (T13-B1).
     */
    public ValidationRun run(Long procedureHistoryId,
                             SyntheticInjectionScenario scenario,
                             String faultCategory,
                             Long targetLruId,
                             Long targetStepId,
                             String recordedBy) {
        if (procedureHistoryId == null) {
            throw new ValidationFixtureException("Procedure revision is required");
        }
        ProcedureHistory history = historyRepository.findById(procedureHistoryId)
                .orElseThrow(() -> new ValidationFixtureException(
                        "Procedure revision not found: " + procedureHistoryId));

        if (!"APPROVED".equalsIgnoreCase(history.getApprovalStatus())) {
            throw new ValidationFixtureException(
                    "Synthetic fault injection requires an APPROVED procedure revision");
        }

        if (targetLruId != null && !targetLruId.equals(history.getLruId())) {
            throw new ValidationFixtureException(
                    "Procedure revision " + procedureHistoryId + " does not belong to LRU " + targetLruId);
        }

        SyntheticFaultCategory parsedCategory = null;
        if (faultCategory != null && !faultCategory.isBlank()) {
            try {
                parsedCategory = SyntheticFaultCategory.valueOf(faultCategory.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new ValidationFixtureException("Unsupported synthetic fault category: " + faultCategory);
            }
        }

        SyntheticInjectionScenario selected = scenario == null
                ? SyntheticInjectionScenario.NOMINAL : scenario;

        if (selected == SyntheticInjectionScenario.FAULT_INJECTED && parsedCategory == null) {
            parsedCategory = SyntheticFaultCategory.OUT_OF_RANGE;
        }

        String categoryLabel = parsedCategory != null ? parsedCategory.name().toLowerCase(Locale.ROOT) : "nominal";
        String filename = "synthetic-" + selected.name().toLowerCase(Locale.ROOT) + "-"
                + categoryLabel + "-" + UUID.randomUUID() + ".json";

        byte[] fixture = buildFixture(history, selected, parsedCategory, targetStepId);
        return ingestionService.ingest(
                procedureHistoryId,
                filename,
                "JSON",
                recordedBy == null || recordedBy.isBlank() ? "SYSTEM" : recordedBy,
                fixture);
    }

    private byte[] buildFixture(ProcedureHistory history,
                                SyntheticInjectionScenario scenario,
                                SyntheticFaultCategory faultCategory,
                                Long targetStepId) {
        try {
            JsonNode steps = objectMapper.readTree(history.getStepsSnapshot());
            if (!steps.isArray() || steps.isEmpty()) {
                throw new ValidationFixtureException("Approved procedure revision has no test steps");
            }

            if (targetStepId != null) {
                boolean foundTarget = false;
                for (JsonNode step : steps) {
                    if (step.hasNonNull("id") && step.get("id").asLong() == targetStepId) {
                        foundTarget = true;
                        break;
                    }
                }
                if (!foundTarget) {
                    throw new ValidationFixtureException(
                            "testStepId " + targetStepId + " is not part of procedure revision " + history.getId());
                }
            }

            ArrayNode readings = objectMapper.createArrayNode();
            boolean injected = false;
            String injectionRunId = UUID.randomUUID().toString();

            for (JsonNode step : steps) {
                if (!step.hasNonNull("id")) {
                    continue;
                }
                long stepId = step.get("id").asLong();
                boolean isTarget = (scenario == SyntheticInjectionScenario.FAULT_INJECTED)
                        && (targetStepId != null ? (stepId == targetStepId) : (!injected));

                if (isTarget) {
                    SyntheticFaultCategory category = faultCategory != null
                            ? faultCategory : SyntheticFaultCategory.OUT_OF_RANGE;

                    switch (category) {
                        case MISSING_SIGNAL -> {
                            // Deliberately omit this reading from the fixture to trigger MISSING outcome
                            injected = true;
                            continue;
                        }
                        case DISCRETE_MISMATCH -> {
                            ObjectNode reading = objectMapper.createObjectNode();
                            reading.put("testStepId", stepId);
                            reading.put("observedBehaviour", "SYNTHETIC_FAULT_DISCRETE_MISMATCH");
                            reading.put("_injectionRunId", injectionRunId);
                            reading.put("_syntheticScenario", scenario.name());
                            reading.put("_faultCategory", category.name());
                            readings.add(reading);
                            injected = true;
                            continue;
                        }
                        case OUT_OF_RANGE -> {
                            ObjectNode reading = objectMapper.createObjectNode();
                            reading.put("testStepId", stepId);
                            if (step.hasNonNull("expectedUnit") && step.hasNonNull("expectedRange")) {
                                EngineeringUnit unit = EngineeringUnit.valueOf(step.get("expectedUnit").asText());
                                ExpectedRange range = ExpectedRange.parse(step.get("expectedRange").asText());
                                BigDecimal width = range.upper().subtract(range.lower()).abs();
                                BigDecimal outOfRangeValue = range.upper().add(
                                        width.signum() == 0 ? BigDecimal.ONE : width.max(BigDecimal.ONE));
                                reading.put("observedValue", outOfRangeValue);
                                reading.put("observedUnit", unit.name());
                            } else if (step.hasNonNull("expectedBehaviour")) {
                                reading.put("observedBehaviour", "SYNTHETIC_FAULT_OUT_OF_RANGE");
                            }
                            reading.put("_injectionRunId", injectionRunId);
                            reading.put("_syntheticScenario", scenario.name());
                            reading.put("_faultCategory", category.name());
                            readings.add(reading);
                            injected = true;
                            continue;
                        }
                    }
                }

                // Nominal generation
                ObjectNode reading = objectMapper.createObjectNode();
                reading.put("testStepId", stepId);
                if (step.hasNonNull("expectedUnit") && step.hasNonNull("expectedRange")) {
                    EngineeringUnit unit = EngineeringUnit.valueOf(step.get("expectedUnit").asText());
                    ExpectedRange range = ExpectedRange.parse(step.get("expectedRange").asText());
                    BigDecimal value = range.lower().add(range.upper())
                            .divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
                    reading.put("observedValue", value);
                    reading.put("observedUnit", unit.name());
                } else if (step.hasNonNull("expectedBehaviour")) {
                    reading.put("observedBehaviour", step.get("expectedBehaviour").asText());
                }
                reading.put("_injectionRunId", injectionRunId);
                reading.put("_syntheticScenario", scenario.name());
                readings.add(reading);
            }

            if (readings.isEmpty() && faultCategory != SyntheticFaultCategory.MISSING_SIGNAL) {
                throw new ValidationFixtureException("Approved procedure revision has no identifiable test steps");
            }
            if (scenario == SyntheticInjectionScenario.FAULT_INJECTED && !injected) {
                throw new ValidationFixtureException("Unable to inject fault: no applicable test step found");
            }

            return objectMapper.writeValueAsBytes(readings);
        } catch (ValidationFixtureException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationFixtureException("Unable to generate synthetic validation evidence", ex);
        }
    }
}
