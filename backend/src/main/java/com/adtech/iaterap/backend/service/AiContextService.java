package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.coverage.CoverageEngine;
import com.adtech.iaterap.backend.coverage.CoverageScore;
import com.adtech.iaterap.backend.coverage.CoverageScoringConfigurationService;
import com.adtech.iaterap.backend.coverage.rule.RuleEngine;
import com.adtech.iaterap.backend.coverage.rule.RuleResult;
import com.adtech.iaterap.backend.dto.AiContextStep;
import com.adtech.iaterap.backend.dto.AiCoverageContext;
import com.adtech.iaterap.backend.dto.AiCoverageGap;
import com.adtech.iaterap.backend.dto.AiProcedureContextResponse;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.entity.TestStep;
import com.adtech.iaterap.backend.repository.FaultTemplateRepository;
import com.adtech.iaterap.backend.repository.FmeaBranchMappingRepository;
import com.adtech.iaterap.backend.repository.InterfaceRepository;
import com.adtech.iaterap.backend.repository.SignalLibraryRepository;
import com.adtech.iaterap.backend.repository.TestProcedureRepository;
import com.adtech.iaterap.backend.repository.TestStepRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/** Builds the read-only AI advisory context without invoking any write service. */
@Service
public class AiContextService {
    public static final String CONTRACT_VERSION = "1.0";

    private final TestProcedureRepository procedures;
    private final TestStepRepository steps;
    private final RuleEngine ruleEngine;
    private final CoverageEngine coverageEngine;
    private final CoverageScoringConfigurationService scoringConfiguration;
    private final InterfaceRepository interfaces;
    private final SignalLibraryRepository signals;
    private final FaultTemplateRepository faults;
    private final FmeaBranchMappingRepository fmeaMappings;

    public AiContextService(
            TestProcedureRepository procedures,
            TestStepRepository steps,
            RuleEngine ruleEngine,
            CoverageEngine coverageEngine,
            CoverageScoringConfigurationService scoringConfiguration,
            InterfaceRepository interfaces,
            SignalLibraryRepository signals,
            FaultTemplateRepository faults,
            FmeaBranchMappingRepository fmeaMappings) {
        this.procedures = procedures;
        this.steps = steps;
        this.ruleEngine = ruleEngine;
        this.coverageEngine = coverageEngine;
        this.scoringConfiguration = scoringConfiguration;
        this.interfaces = interfaces;
        this.signals = signals;
        this.faults = faults;
        this.fmeaMappings = fmeaMappings;
    }

    @Transactional(readOnly = true)
    public AiProcedureContextResponse getProcedureContext(Long procedureId) {
        TestProcedure procedure = procedures.findById(procedureId)
                .orElseThrow(() -> new NoSuchElementException("Test procedure not found: " + procedureId));
        List<TestStep> orderedSteps = steps.findByTestProcedureIdOrderByExecutionOrderAsc(procedureId);
        Map<String, RuleResult> results = ruleEngine.evaluate(procedure, procedure.getLru(), orderedSteps);
        CoverageScore score = coverageEngine.calculate(results);

        List<AiCoverageGap> gaps = new ArrayList<>();
        results.forEach((ruleId, result) -> {
            Object gapIds = result.getDetails().get("gapIds");
            if (gapIds instanceof List<?> ids) {
                for (Object id : ids) {
                    if (id instanceof Number number) {
                        String type = ruleId.replace("COVERAGE-", "");
                        gaps.add(new AiCoverageGap(type, number.longValue(), describe(type, number.longValue())));
                    }
                }
            }
        });

        Map<String, Double> weights = new LinkedHashMap<>();
        scoringConfiguration.activeCoverageRules().forEach(rule ->
                weights.put(rule.ruleId(), rule.weight().movePointLeft(2).doubleValue()));

        AiCoverageContext coverage = new AiCoverageContext(
                score.getCategoryScores(),
                weights,
                score.getCompositeScore(),
                score.getThresholdResults(),
                score.getUnavailableReasons(),
                gaps);

        return new AiProcedureContextResponse(
                CONTRACT_VERSION,
                procedure.getId(),
                procedure.getName(),
                procedure.getDescription(),
                procedure.getLru() == null ? null : procedure.getLru().getId(),
                procedure.getApprovalStatus(),
                procedure.getVersionNumber(),
                orderedSteps.size(),
                orderedSteps.stream().map(AiContextStep::fromEntity).toList(),
                coverage,
                Instant.now());
    }

    private String describe(String type, Long id) {
        return switch (type) {
            case "INTERFACE" -> interfaces.findById(id)
                    .map(value -> "Unreferenced interface: " + value.getName())
                    .orElse("Unreferenced interface " + id);
            case "SIGNAL" -> signals.findById(id)
                    .map(value -> "Unreferenced signal: " + value.getName())
                    .orElse("Unreferenced signal " + id);
            case "FAULT" -> faults.findById(id)
                    .map(value -> "Unreferenced fault: " + value.getName() + " (" + value.getFaultCode() + ")")
                    .orElse("Unreferenced fault " + id);
            case "FMEA" -> fmeaMappings.findById(id)
                    .map(value -> "Unreferenced FMEA branch: " + value.getFmeaBranchCode() + " — " + value.getFailureEffect())
                    .orElse("Unreferenced FMEA branch " + id);
            default -> "Unreferenced " + type.toLowerCase() + " element " + id;
        };
    }
}
