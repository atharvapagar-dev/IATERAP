package com.adtech.iaterap.backend.coverage;

import com.adtech.iaterap.backend.coverage.rule.RuleResult;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoverageEngineTest {
    @Test
    void calculatesWeightedCompositeUsingAllSixCategories() {
        Map<String, RuleResult> results = new LinkedHashMap<>();
        CoverageEngine.getWeights().keySet().forEach(category ->
                results.put(category, RuleResult.pass("ok", Map.of("percentage", score(category)))));

        CoverageScore score = CoverageEngine.calculate(results);

        assertEquals(65.0, score.getCompositeScore());
        assertEquals(100.0, score.getCategoryScores().get("COVERAGE-FAULT"));
        assertEquals(0.0, score.getCategoryScores().get("COVERAGE-FUNCTIONAL"));
    }

    @Test
    void rejectsIncompleteCategoryResults() {
        assertThrows(IllegalStateException.class, () -> CoverageEngine.calculate(Map.of()));
    }

    private double score(String category) {
        return category.equals("COVERAGE-FAULT") || category.equals("COVERAGE-FMEA")
                || category.equals("COVERAGE-REQUIREMENT") ? 100.0 : 0.0;
    }
}
