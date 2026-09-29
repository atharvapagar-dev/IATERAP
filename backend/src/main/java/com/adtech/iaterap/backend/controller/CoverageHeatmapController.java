package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.coverage.CoverageEngine;
import com.adtech.iaterap.backend.coverage.CoverageScore;
import com.adtech.iaterap.backend.coverage.rule.RuleEngine;
import com.adtech.iaterap.backend.dto.CoverageHeatmapRow;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.repository.TestProcedureRepository;
import com.adtech.iaterap.backend.repository.TestStepRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Aggregates deterministic coverage scores for the dashboard heatmap. */
@RestController
@RequestMapping("/api/coverage")
public class CoverageHeatmapController {
    private final TestProcedureRepository procedures;
    private final TestStepRepository steps;
    private final RuleEngine ruleEngine;
    private final CoverageEngine coverageEngine;

    public CoverageHeatmapController(TestProcedureRepository procedures, TestStepRepository steps,
                                     RuleEngine ruleEngine, CoverageEngine coverageEngine) {
        this.procedures = procedures;
        this.steps = steps;
        this.ruleEngine = ruleEngine;
        this.coverageEngine = coverageEngine;
    }

    @GetMapping("/heatmap")
    public List<CoverageHeatmapRow> heatmap() {
        return procedures.findAll().stream()
                .map(this::procedure)
                .collect(Collectors.groupingBy(ProcedureCoverage::subsystemName))
                .entrySet().stream()
                .map(this::row)
                .sorted(Comparator.comparing(CoverageHeatmapRow::subsystemName))
                .toList();
    }

    private ProcedureCoverage procedure(TestProcedure procedure) {
        var results = ruleEngine.evaluate(procedure, procedure.getLru(),
                steps.findByTestProcedureIdOrderByExecutionOrderAsc(procedure.getId()));
        CoverageScore score = coverageEngine.calculate(results);
        String subsystem = procedure.getLru() == null || procedure.getLru().getSubsystem() == null
                ? "Unassigned" : procedure.getLru().getSubsystem().getName();
        return new ProcedureCoverage(subsystem, procedure.getId(), procedure.getName(),
                procedure.getLru() == null ? "Unassigned" : procedure.getLru().getName(), status(score.getCategoryScores()));
    }

    private CoverageHeatmapRow row(Map.Entry<String, List<ProcedureCoverage>> group) {
        List<CoverageHeatmapRow.CoverageHeatmapProcedure> rows = group.getValue().stream()
                .map(item -> new CoverageHeatmapRow.CoverageHeatmapProcedure(item.procedureId(), item.procedureName(), item.lruName(), item.status()))
                .toList();
        return new CoverageHeatmapRow(group.getKey(),
                group.getValue().stream().filter(item -> item.status().equals("COVERED")).count(),
                group.getValue().stream().filter(item -> item.status().equals("PARTIALLY_COVERED")).count(),
                group.getValue().stream().filter(item -> item.status().equals("NOT_COVERED")).count(), rows);
    }

    private String status(Map<String, Double> categories) {
        List<Double> measured = categories.values().stream().filter(java.util.Objects::nonNull).toList();
        if (measured.isEmpty() || measured.stream().allMatch(score -> score == 0D)) return "NOT_COVERED";
        if (measured.stream().allMatch(score -> score >= 100D)) return "COVERED";
        return "PARTIALLY_COVERED";
    }

    private record ProcedureCoverage(String subsystemName, Long procedureId, String procedureName, String lruName, String status) { }
}
