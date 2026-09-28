package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.ProcedureHistory;
import com.adtech.iaterap.backend.entity.ValidationRun;
import com.adtech.iaterap.backend.repository.ProcedureHistoryRepository;
import com.adtech.iaterap.backend.validation.ExpectedRange;
import com.adtech.iaterap.backend.validation.EngineeringUnit;
import com.adtech.iaterap.backend.validation.SyntheticInjectionScenario;
import com.adtech.iaterap.backend.validation.ValidationFixtureException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Generates explicitly synthetic evidence for one immutable procedure revision.
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

    public ValidationRun run(Long procedureHistoryId, SyntheticInjectionScenario scenario,
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
        SyntheticInjectionScenario selected = scenario == null
                ? SyntheticInjectionScenario.NOMINAL : scenario;
        byte[] fixture = buildFixture(history, selected);
        return ingestionService.ingest(procedureHistoryId,
                "synthetic-" + selected.name().toLowerCase() + "-" + UUID.randomUUID() + ".json",
                "JSON", recordedBy == null || recordedBy.isBlank() ? "SYSTEM" : recordedBy,
                fixture);
    }

    private byte[] buildFixture(ProcedureHistory history, SyntheticInjectionScenario scenario) {
        try {
            var steps = objectMapper.readTree(history.getStepsSnapshot());
            if (!steps.isArray() || steps.isEmpty()) {
                throw new ValidationFixtureException("Approved procedure revision has no test steps");
            }
            ArrayNode readings = objectMapper.createArrayNode();
            boolean injected = false;
            for (var step : steps) {
                if (!step.hasNonNull("id")) {
                    continue;
                }
                ObjectNode reading = objectMapper.createObjectNode();
                reading.put("testStepId", step.get("id").asLong());
                if (step.hasNonNull("expectedUnit") && step.hasNonNull("expectedRange")) {
                    EngineeringUnit unit = EngineeringUnit.valueOf(step.get("expectedUnit").asText());
                    ExpectedRange range = ExpectedRange.parse(step.get("expectedRange").asText());
                    BigDecimal value = range.lower().add(range.upper())
                            .divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
                    if (scenario == SyntheticInjectionScenario.FAULT_INJECTED && !injected) {
                        BigDecimal width = range.upper().subtract(range.lower()).abs();
                        value = range.upper().add(width.signum() == 0
                                ? BigDecimal.ONE : width.max(BigDecimal.ONE));
                        injected = true;
                    }
                    reading.put("observedValue", value);
                    reading.put("observedUnit", unit.name());
                } else if (step.hasNonNull("expectedBehaviour")) {
                    String behaviour = step.get("expectedBehaviour").asText();
                    if (scenario == SyntheticInjectionScenario.FAULT_INJECTED && !injected) {
                        behaviour = "SYNTHETIC_FAULT_INJECTED";
                        injected = true;
                    }
                    reading.put("observedBehaviour", behaviour);
                }
                reading.put("_injectionRunId", UUID.randomUUID().toString());
                readings.add(reading);
            }
            if (readings.isEmpty()) {
                throw new ValidationFixtureException("Approved procedure revision has no identifiable test steps");
            }
            return objectMapper.writeValueAsBytes(readings);
        } catch (ValidationFixtureException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationFixtureException("Unable to generate synthetic validation evidence", ex);
        }
    }
}
