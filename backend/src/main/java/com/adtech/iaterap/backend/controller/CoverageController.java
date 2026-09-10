package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.coverage.CoverageEngine;
import com.adtech.iaterap.backend.coverage.CoverageScore;
import com.adtech.iaterap.backend.coverage.CoverageScoringConfigurationService;
import com.adtech.iaterap.backend.dto.CoverageResponse;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.repository.TestProcedureRepository;
import com.adtech.iaterap.backend.repository.TestStepRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/** Read-only procedure coverage endpoint for the Procedure Builder and demo flow. */
@RestController
@RequestMapping("/api/procedures")
public class CoverageController {
    private final TestProcedureRepository procedureRepository;
    private final TestStepRepository stepRepository;
    private final CoverageEngine coverageEngine;
    private final CoverageScoringConfigurationService scoringConfigurationService;

    public CoverageController(
            TestProcedureRepository procedureRepository,
            TestStepRepository stepRepository,
            CoverageEngine coverageEngine,
            CoverageScoringConfigurationService scoringConfigurationService) {
        this.procedureRepository = procedureRepository;
        this.stepRepository = stepRepository;
        this.coverageEngine = coverageEngine;
        this.scoringConfigurationService = scoringConfigurationService;
    }

    @GetMapping("/{procedureId}/coverage")
    public CoverageResponse getCoverage(@PathVariable Long procedureId) {
        TestProcedure procedure = procedureRepository.findById(procedureId)
                .orElseThrow(() -> new NoSuchElementException("Test procedure not found: " + procedureId));

        CoverageScore score = coverageEngine.evaluate(
                procedure,
                procedure.getLru(),
                stepRepository.findByTestProcedureIdOrderByExecutionOrderAsc(procedureId));

        Map<String, Double> weights = new LinkedHashMap<>();
        scoringConfigurationService.activeCoverageRules().forEach(rule ->
                weights.put(rule.ruleId(), rule.weight().movePointLeft(2).doubleValue()));

        return new CoverageResponse(
                procedure.getId(),
                procedure.getVersionNumber(),
                score.getCategoryScores(),
                weights,
                score.getCompositeScore(),
                score.getThresholdResults(),
                score.getUnavailableReasons());
    }
}
