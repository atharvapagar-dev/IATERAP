package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.ValidationReportResponse;
import com.adtech.iaterap.backend.entity.ProcedureHistory;
import com.adtech.iaterap.backend.entity.ValidationReading;
import com.adtech.iaterap.backend.entity.ValidationRun;
import com.adtech.iaterap.backend.repository.ProcedureHistoryRepository;
import com.adtech.iaterap.backend.repository.ValidationReadingRepository;
import com.adtech.iaterap.backend.repository.ValidationRunRepository;
import com.adtech.iaterap.backend.service.ValidationIngestionService;
import com.adtech.iaterap.backend.validation.ValidationFixtureException;
import com.adtech.iaterap.backend.validation.SyntheticInjectionScenario;
import com.adtech.iaterap.backend.service.SyntheticFaultInjectionService;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** API boundary for independent validation fixtures and discrepancy evidence (T13-B1, T13-B2). */
@RestController
@RequestMapping("/api/validation")
public class ValidationController {

    private static final long MAX_FIXTURE_BYTES = 2 * 1024 * 1024;

    private final ValidationIngestionService ingestionService;
    private final ValidationRunRepository runRepository;
    private final ValidationReadingRepository readingRepository;
    private final ProcedureHistoryRepository historyRepository;
    private final SyntheticFaultInjectionService syntheticFaultInjectionService;

    public ValidationController(
            ValidationIngestionService ingestionService,
            ValidationRunRepository runRepository,
            ValidationReadingRepository readingRepository,
            ProcedureHistoryRepository historyRepository,
            SyntheticFaultInjectionService syntheticFaultInjectionService) {
        this.ingestionService = ingestionService;
        this.runRepository = runRepository;
        this.readingRepository = readingRepository;
        this.historyRepository = historyRepository;
        this.syntheticFaultInjectionService = syntheticFaultInjectionService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ENGINEER', 'RELIABILITY_ENGINEER', 'TEST_ENGINEER')")
    @PostMapping("/runs/synthetic")
    public ValidationRun runSynthetic(
            @RequestParam Long procedureHistoryId,
            @RequestParam(defaultValue = "NOMINAL") SyntheticInjectionScenario scenario,
            @RequestParam(required = false) String faultCategory,
            @RequestParam(required = false) Long targetLruId,
            @RequestParam(required = false) Long targetStepId,
            Principal principal) {
        String recordedBy = principal == null ? "SYSTEM" : principal.getName();
        return syntheticFaultInjectionService.run(
                procedureHistoryId, scenario, faultCategory, targetLruId, targetStepId, recordedBy);
    }

    @PostMapping(value = "/runs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ValidationRun upload(
            @RequestParam Long procedureHistoryId,
            @RequestParam String format,
            @RequestParam MultipartFile file,
            Principal principal) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ValidationFixtureException("Validation fixture file is required");
        }
        if (file.getSize() > MAX_FIXTURE_BYTES) {
            throw new ValidationFixtureException("Validation fixture exceeds the 2 MB limit");
        }
        String normalizedFormat = format.trim().toUpperCase(Locale.ROOT);
        if (!normalizedFormat.equals("CSV") && !normalizedFormat.equals("JSON")) {
            throw new ValidationFixtureException("Validation fixture format must be CSV or JSON");
        }
        String recordedBy = principal == null ? "SYSTEM" : principal.getName();
        return ingestionService.ingest(
                procedureHistoryId, file.getOriginalFilename(), normalizedFormat,
                recordedBy, file.getBytes());
    }

    @GetMapping("/runs/{runId}")
    public ValidationRun getRun(@PathVariable Long runId) {
        return runRepository.findById(runId)
                .orElseThrow(() -> new ValidationFixtureException("Validation run not found: " + runId));
    }

    @GetMapping("/runs")
    public List<ValidationRun> getRuns(@RequestParam Long procedureHistoryId) {
        return runRepository.findByProcedureHistoryIdOrderByUploadedAtDesc(procedureHistoryId);
    }

    @GetMapping("/runs/{runId}/readings")
    public List<ValidationReading> getReadings(@PathVariable Long runId) {
        if (!runRepository.existsById(runId)) {
            throw new ValidationFixtureException("Validation run not found: " + runId);
        }
        return readingRepository.findByValidationRunIdOrderBySourceRowNumberAsc(runId);
    }

    @GetMapping({"/runs/{runId}/report", "/reports/{runId}"})
    public ValidationReportResponse getReport(@PathVariable Long runId) {
        ValidationRun run = runRepository.findById(runId)
                .orElseThrow(() -> new ValidationFixtureException("Validation run not found: " + runId));
        ProcedureHistory history = historyRepository.findById(run.getProcedureHistoryId())
                .orElse(null);
        List<ValidationReading> readings = readingRepository.findByValidationRunIdOrderBySourceRowNumberAsc(runId);
        return ValidationReportResponse.of(run, history, readings);
    }

    @GetMapping("/reports")
    public List<ValidationReportResponse> listReports(@RequestParam Long procedureHistoryId) {
        List<ValidationRun> runs = runRepository.findByProcedureHistoryIdOrderByUploadedAtDesc(procedureHistoryId);
        return runs.stream().map(run -> {
            ProcedureHistory history = historyRepository.findById(run.getProcedureHistoryId()).orElse(null);
            List<ValidationReading> readings = readingRepository.findByValidationRunIdOrderBySourceRowNumberAsc(run.getId());
            return ValidationReportResponse.of(run, history, readings);
        }).toList();
    }
}
