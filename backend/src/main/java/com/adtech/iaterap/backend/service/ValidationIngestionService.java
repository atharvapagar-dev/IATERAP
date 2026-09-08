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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Persists an external fixture without prematurely claiming it passed validation. */
@Service
public class ValidationIngestionService {

    private final ProcedureHistoryRepository procedureHistoryRepository;
    private final ValidationRunRepository validationRunRepository;
    private final ValidationReadingRepository validationReadingRepository;
    private final ValidationFixtureParser fixtureParser;

    public ValidationIngestionService(
            ProcedureHistoryRepository procedureHistoryRepository,
            ValidationRunRepository validationRunRepository,
            ValidationReadingRepository validationReadingRepository,
            ValidationFixtureParser fixtureParser) {
        this.procedureHistoryRepository = procedureHistoryRepository;
        this.validationRunRepository = validationRunRepository;
        this.validationReadingRepository = validationReadingRepository;
        this.fixtureParser = fixtureParser;
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

        Set<Long> revisionStepIds = stepIds(history);
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
        run.setTotalReadings(inputs.size());
        run = validationRunRepository.save(run);

        ValidationRun savedRun = run;
        List<ValidationReading> readings = inputs.stream().map(input -> {
            ValidationReading reading = new ValidationReading();
            reading.setValidationRunId(savedRun.getId());
            reading.setTestStepId(input.testStepId());
            reading.setSourceRowNumber(input.sourceRowNumber());
            reading.setObservedValue(input.observedValue());
            reading.setObservedUnit(input.observedUnit());
            reading.setObservedBehaviour(input.observedBehaviour());
            reading.setResult(ValidationResult.PENDING);
            return reading;
        }).toList();
        validationReadingRepository.saveAll(readings);
        return run;
    }

    private Set<Long> stepIds(ProcedureHistory history) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(history.getStepsSnapshot());
            if (!root.isArray()) {
                throw new ValidationFixtureException("Procedure revision has an invalid steps snapshot");
            }
            Set<Long> ids = new HashSet<>();
            root.forEach(step -> {
                if (step.hasNonNull("id")) {
                    ids.add(step.get("id").asLong());
                }
            });
            return ids;
        } catch (ValidationFixtureException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationFixtureException("Procedure revision has an unreadable steps snapshot", ex);
        }
    }
}
