package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.coverage.CoverageEngine;
import com.adtech.iaterap.backend.coverage.CoverageScore;
import com.adtech.iaterap.backend.coverage.rule.RuleEngine;
import com.adtech.iaterap.backend.dto.CoverageHeatmapRow;
import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.Subsystem;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.repository.TestProcedureRepository;
import com.adtech.iaterap.backend.repository.TestStepRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CoverageHeatmapControllerTest {
    @Test
    void aggregatesCoveredPartialAndNotCoveredProceduresBySubsystem() throws Exception {
        TestProcedureRepository procedures = mock(TestProcedureRepository.class);
        TestStepRepository steps = mock(TestStepRepository.class);
        RuleEngine rules = mock(RuleEngine.class);
        CoverageEngine engine = mock(CoverageEngine.class);
        when(procedures.findAll()).thenReturn(List.of(
                procedure(1L, "Covered", "Flight Controls"),
                procedure(2L, "Partial", "Flight Controls"),
                procedure(3L, "Missing", "Flight Controls"),
                procedure(4L, "Unassigned", null)));
        when(steps.findByTestProcedureIdOrderByExecutionOrderAsc(any())).thenReturn(List.of());
        when(rules.evaluate(any(), any(), any())).thenReturn(Map.of());
        when(engine.calculate(any())).thenReturn(score(100D), score(50D), score(0D), score(0D));

        List<CoverageHeatmapRow> heatmap = new CoverageHeatmapController(procedures, steps, rules, engine).heatmap();

        assertEquals(2, heatmap.size());
        assertEquals("Flight Controls", heatmap.get(0).subsystemName());
        assertEquals(1, heatmap.get(0).coveredCount());
        assertEquals(1, heatmap.get(0).partiallyCoveredCount());
        assertEquals(1, heatmap.get(0).notCoveredCount());
        assertEquals(List.of("COVERED", "PARTIALLY_COVERED", "NOT_COVERED"), heatmap.get(0).procedures().stream()
                .map(CoverageHeatmapRow.CoverageHeatmapProcedure::status).toList());
        assertEquals("Unassigned", heatmap.get(1).subsystemName());
        assertEquals(1, heatmap.get(1).notCoveredCount());
    }

    private CoverageScore score(double interfaceScore) {
        Map<String, Double> categories = new LinkedHashMap<>();
        categories.put("COVERAGE-INTERFACE", interfaceScore);
        categories.put("COVERAGE-SIGNAL", null);
        return new CoverageScore(categories, null, Map.of(), Map.of());
    }

    private TestProcedure procedure(Long id, String name, String subsystemName) throws Exception {
        Lru lru = new Lru("LRU " + id);
        if (subsystemName != null) lru.setSubsystem(new Subsystem(subsystemName));
        TestProcedure procedure = new TestProcedure(); procedure.setLru(lru); procedure.setName(name);
        Field field = TestProcedure.class.getDeclaredField("id"); field.setAccessible(true); field.set(procedure, id);
        return procedure;
    }
}
