package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.entity.ValidationReading;
import com.adtech.iaterap.backend.entity.ValidationRun;
import com.adtech.iaterap.backend.repository.ValidationReadingRepository;
import com.adtech.iaterap.backend.repository.ValidationRunRepository;
import com.adtech.iaterap.backend.service.ValidationIngestionService;
import com.adtech.iaterap.backend.validation.ValidationFixtureException;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** API boundary for independent validation fixtures and discrepancy evidence. */
@RestController
@RequestMapping("/api/validation")
public class ValidationController {

    private static final long MAX_FIXTURE_BYTES = 2 * 1024 * 1024;

    private final ValidationIngestionService ingestionService;
    private final ValidationRunRepository runRepository;
    private final ValidationReadingRepository readingRepository;

    public ValidationController(
            ValidationIngestionService ingestionService,
            ValidationRunRepository runRepository,
            ValidationReadingRepository readingRepository) {
        this.ingestionService = ingestionService;
        this.runRepository = runRepository;
        this.readingRepository = readingRepository;
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

    @GetMapping("/runs/{runId}/readings")
    public List<ValidationReading> getReadings(@PathVariable Long runId) {
        if (!runRepository.existsById(runId)) {
            throw new ValidationFixtureException("Validation run not found: " + runId);
        }
        return readingRepository.findByValidationRunIdOrderBySourceRowNumberAsc(runId);
    }
}
