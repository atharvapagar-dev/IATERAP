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

import java.util.List;

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
        return procedures.findAll().stream().map(this::row).toList();
    }

    private CoverageHeatmapRow row(TestProcedure procedure) {
        var results = ruleEngine.evaluate(procedure, procedure.getLru(),
                steps.findByTestProcedureIdOrderByExecutionOrderAsc(procedure.getId()));
        CoverageScore score = coverageEngine.calculate(results);
        return new CoverageHeatmapRow(procedure.getId(), procedure.getName(),
                procedure.getLru() == null ? "Unassigned" : procedure.getLru().getName(),
                procedure.getLru() == null || procedure.getLru().getSubsystem() == null
                        ? "Unassigned" : procedure.getLru().getSubsystem().getName(),
                score.getCategoryScores(), score.getCompositeScore());
    }
}
