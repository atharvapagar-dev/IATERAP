package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.coverage.CoverageEngine;
import com.adtech.iaterap.backend.coverage.CoverageScore;
import com.adtech.iaterap.backend.coverage.CoverageScoringConfigurationService;
import com.adtech.iaterap.backend.coverage.rule.RuleEngine;
import com.adtech.iaterap.backend.coverage.rule.RuleResult;
import com.adtech.iaterap.backend.dto.CoverageGapResponse;
import com.adtech.iaterap.backend.dto.CoverageResponse;
import com.adtech.iaterap.backend.dto.ApiResponse;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.repository.TestProcedureRepository;
import com.adtech.iaterap.backend.repository.TestStepRepository;
import com.adtech.iaterap.backend.repository.InterfaceRepository;
import com.adtech.iaterap.backend.repository.SignalLibraryRepository;
import com.adtech.iaterap.backend.repository.FaultTemplateRepository;
import com.adtech.iaterap.backend.repository.FmeaBranchMappingRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.util.List;

/** Read-only procedure coverage endpoint for the Procedure Builder and demo flow. */
@RestController
@RequestMapping("/api/procedures")
public class CoverageController {
    private final TestProcedureRepository procedureRepository;
    private final TestStepRepository stepRepository;
    private final CoverageEngine coverageEngine;
    private final CoverageScoringConfigurationService scoringConfigurationService;
    private final RuleEngine ruleEngine;
    private final InterfaceRepository interfaceRepository;
    private final SignalLibraryRepository signalRepository;
    private final FaultTemplateRepository faultRepository;
    private final FmeaBranchMappingRepository fmeaRepository;

    public CoverageController(
            TestProcedureRepository procedureRepository,
            TestStepRepository stepRepository,
            CoverageEngine coverageEngine,
            CoverageScoringConfigurationService scoringConfigurationService,
            RuleEngine ruleEngine, InterfaceRepository interfaceRepository,
            SignalLibraryRepository signalRepository, FaultTemplateRepository faultRepository,
            FmeaBranchMappingRepository fmeaRepository) {
        this.procedureRepository = procedureRepository;
        this.stepRepository = stepRepository;
        this.coverageEngine = coverageEngine;
        this.scoringConfigurationService = scoringConfigurationService;
        this.ruleEngine = ruleEngine;
        this.interfaceRepository = interfaceRepository;
        this.signalRepository = signalRepository;
        this.faultRepository = faultRepository;
        this.fmeaRepository = fmeaRepository;
    }

    @GetMapping("/{procedureId}/coverage")
    public ApiResponse<CoverageResponse> getCoverage(@PathVariable Long procedureId) {
        TestProcedure procedure = procedureRepository.findById(procedureId)
                .orElseThrow(() -> new NoSuchElementException("Test procedure not found: " + procedureId));

        var steps = stepRepository.findByTestProcedureIdOrderByExecutionOrderAsc(procedureId);
        Map<String, RuleResult> results = ruleEngine.evaluate(procedure, procedure.getLru(), steps);
        CoverageScore score = coverageEngine.calculate(results);

        Map<String, Double> weights = new LinkedHashMap<>();
        scoringConfigurationService.activeCoverageRules().forEach(rule ->
                weights.put(rule.ruleId(), rule.weight().movePointLeft(2).doubleValue()));

        List<CoverageGapResponse> gaps = new ArrayList<>();
        results.forEach((ruleId, result) -> {
            Object ids = result.getDetails().get("gapIds");
            if (ids instanceof List<?> gapIds) {
                gapIds.forEach(id -> {
                    if (id instanceof Number number) {
                        String type = ruleId.replace("COVERAGE-", "");
                        gaps.add(new CoverageGapResponse(type, number.longValue(), describe(type, number.longValue())));
                    }
                });
            }
        });

        return ApiResponse.of(new CoverageResponse(
                procedure.getId(),
                procedure.getVersionNumber(),
                score.getCategoryScores(),
                weights,
                score.getCompositeScore(),
                score.getThresholdResults(),
                score.getUnavailableReasons(), gaps));
    }

    private String describe(String type, Long id) {
        return switch (type) {
            case "INTERFACE" -> interfaceRepository.findById(id).map(v -> "Unreferenced interface: " + v.getName()).orElse("Unreferenced interface " + id);
            case "SIGNAL" -> signalRepository.findById(id).map(v -> "Unreferenced signal: " + v.getName()).orElse("Unreferenced signal " + id);
            case "FAULT" -> faultRepository.findById(id).map(v -> "Unreferenced fault: " + v.getName() + " (" + v.getFaultCode() + ")").orElse("Unreferenced fault " + id);
            case "FMEA" -> fmeaRepository.findById(id).map(v -> "Unreferenced FMEA branch: " + v.getFmeaBranchCode() + " — " + v.getFailureEffect()).orElse("Unreferenced FMEA branch " + id);
            default -> "Unreferenced " + type.toLowerCase() + " element " + id;
        };
    }
}
