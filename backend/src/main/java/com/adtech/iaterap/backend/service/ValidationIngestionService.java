package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.ProcedureHistory;
import com.adtech.iaterap.backend.entity.ValidationReading;
import com.adtech.iaterap.backend.entity.ValidationRun;
import com.adtech.iaterap.backend.repository.ProcedureHistoryRepository;
import com.adtech.iaterap.backend.repository.ValidationReadingRepository;
import com.adtech.iaterap.backend.repository.ValidationRunRepository;
import com.adtech.iaterap.backend.validation.ValidationFixtureException;
import com.adtech.iaterap.backend.validation.ValidationFixtureParser;
import com.adtech.iaterap.backend.validation.ValidationReadingInput;
import com.adtech.iaterap.backend.validation.ValidationResult;
import jakarta.transaction.Transactional;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Persists an external fixture without prematurely claiming it passed validation. */
@Service
public class ValidationIngestionService {

    private final ProcedureHistoryRepository procedureHistoryRepository;
    private final ValidationRunRepository validationRunRepository;
    private final ValidationReadingRepository validationReadingRepository;
    private final ValidationFixtureParser fixtureParser;
    private final com.adtech.iaterap.backend.validation.ValidationComparisonService comparisonService;

    public ValidationIngestionService(
            ProcedureHistoryRepository procedureHistoryRepository,
            ValidationRunRepository validationRunRepository,
            ValidationReadingRepository validationReadingRepository,
            ValidationFixtureParser fixtureParser,
            com.adtech.iaterap.backend.validation.ValidationComparisonService comparisonService) {
        this.procedureHistoryRepository = procedureHistoryRepository;
        this.validationRunRepository = validationRunRepository;
        this.validationReadingRepository = validationReadingRepository;
        this.fixtureParser = fixtureParser;
        this.comparisonService = comparisonService;
    }

    @Transactional
    public ValidationRun ingest(
            Long procedureHistoryId,
            String sourceFilename,
            String sourceFormat,
            String recordedBy,
            byte[] content) {
        if (procedureHistoryId == null || sourceFilename == null || sourceFilename.isBlank()
                || recordedBy == null || recordedBy.isBlank()) {
            throw new ValidationFixtureException(
                    "Procedure revision, source filename, and recordedBy are required");
        }

        ProcedureHistory history = procedureHistoryRepository.findById(procedureHistoryId)
                .orElseThrow(() -> new ValidationFixtureException(
                        "Procedure revision not found: " + procedureHistoryId));
        List<ValidationReadingInput> inputs = fixtureParser.parse(content, sourceFormat);
        String checksum = fixtureParser.sha256(content);
        if (validationRunRepository.findByProcedureHistoryIdAndSourceSha256(
                procedureHistoryId, checksum).isPresent()) {
            throw new ValidationFixtureException(
                    "This fixture has already been uploaded for procedure revision "
                            + procedureHistoryId);
        }

        Map<Long, ExpectedContract> contracts = expectedContracts(history);
        Set<Long> revisionStepIds = contracts.keySet();
        for (ValidationReadingInput input : inputs) {
            if (!revisionStepIds.contains(input.testStepId())) {
                throw new ValidationFixtureException(
                        "testStepId " + input.testStepId()
                                + " is not part of procedure revision " + procedureHistoryId);
            }
        }

        ValidationRun run = new ValidationRun();
        run.setProcedureHistoryId(procedureHistoryId);
        run.setSourceFilename(sourceFilename.trim());
        run.setSourceSha256(checksum);
        run.setSourceFormat(sourceFormat.toUpperCase(Locale.ROOT));
        run.setRecordedBy(recordedBy.trim());
        run.setTotalReadings(revisionStepIds.size());

        Map<Long, ValidationReadingInput> inputByStep = inputs.stream()
                .collect(java.util.stream.Collectors.toMap(
                        ValidationReadingInput::testStepId, input -> input));
        Map<ValidationResult, Integer> counts = new EnumMap<>(ValidationResult.class);
        List<ValidationReading> readings = contracts.entrySet().stream().map(entry -> {
            Long stepId = entry.getKey();
            ExpectedContract contract = entry.getValue();
            ValidationReadingInput input = inputByStep.get(stepId);
            var comparison = comparisonService.compare(
                    contract.expectedRange(), contract.expectedUnit(),
                    contract.expectedBehaviour(), input);
            counts.merge(comparison.result(), 1, Integer::sum);
            ValidationReading reading = new ValidationReading();
            reading.setTestStepId(stepId);
            if (input != null) {
                reading.setSourceRowNumber(input.sourceRowNumber());
                reading.setObservedValue(input.observedValue());
                reading.setObservedUnit(input.observedUnit());
                reading.setObservedBehaviour(input.observedBehaviour());
            }
            reading.setNormalizedValue(comparison.normalizedValue());
            reading.setResult(comparison.result());
            reading.setDeviation(comparison.deviation());
            reading.setErrorMessage(comparison.errorMessage());
            return reading;
        }).toList();
        run.setMatchedCount(counts.getOrDefault(ValidationResult.MATCH, 0));
        run.setMismatchedCount(counts.getOrDefault(ValidationResult.MISMATCH, 0));
        run.setOutlierCount(counts.getOrDefault(ValidationResult.OUTLIER, 0));
        run.setMissingCount(counts.getOrDefault(ValidationResult.MISSING, 0));
        run.setStatus(com.adtech.iaterap.backend.validation.ValidationRunStatus.VALIDATED);
        run = validationRunRepository.save(run);

        ValidationRun savedRun = run;
        readings.forEach(reading -> {
            reading.setValidationRunId(savedRun.getId());
        });
        validationReadingRepository.saveAll(readings);
        return run;
    }

    private Map<Long, ExpectedContract> expectedContracts(ProcedureHistory history) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(history.getStepsSnapshot());
            if (!root.isArray()) {
                throw new ValidationFixtureException("Procedure revision has an invalid steps snapshot");
            }
            Map<Long, ExpectedContract> contracts = new java.util.LinkedHashMap<>();
            root.forEach(step -> {
                if (step.hasNonNull("id")) {
                    Long id = step.get("id").asLong();
                    String expectedUnit = step.hasNonNull("expectedUnit")
                            ? step.get("expectedUnit").asText() : null;
                    contracts.put(id, new ExpectedContract(
                            textOrNull(step, "expectedRange"),
                            expectedUnit == null ? null
                                    : com.adtech.iaterap.backend.validation.EngineeringUnit
                                            .valueOf(expectedUnit),
                            textOrNull(step, "expectedBehaviour")));
                }
            });
            return contracts;
        } catch (ValidationFixtureException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationFixtureException("Procedure revision has an unreadable steps snapshot", ex);
        }
    }

    private static String textOrNull(com.fasterxml.jackson.databind.JsonNode node, String field) {
        com.fasterxml.jackson.databind.JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private record ExpectedContract(
            String expectedRange,
            com.adtech.iaterap.backend.validation.EngineeringUnit expectedUnit,
            String expectedBehaviour) {
    }
}
