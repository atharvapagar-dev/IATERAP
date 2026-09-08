package com.adtech.iaterap.backend.coverage;

import com.adtech.iaterap.backend.coverage.rule.RuleResult;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void matchesHandComputedMixedCoverageValue() {
        Map<String, Double> percentages = Map.of(
                "COVERAGE-FAULT", 80.0,
                "COVERAGE-FMEA", 50.0,
                "COVERAGE-REQUIREMENT", 100.0,
                "COVERAGE-INTERFACE", 0.0,
                "COVERAGE-SIGNAL", 25.0,
                "COVERAGE-FUNCTIONAL", 75.0);

        CoverageScore score = CoverageEngine.calculate(results(percentages));

        // 80*.25 + 50*.20 + 100*.20 + 0*.15 + 25*.10 + 75*.10 = 60
        assertEquals(60.0, score.getCompositeScore());
        assertEquals(25.0, score.getCategoryScores().get("COVERAGE-SIGNAL"));
    }

    @Test
    void returnsZeroForZeroElementCategoriesAndFullyUncoveredProcedure() {
        Map<String, RuleResult> zeroCategoryResults = results(Map.of(
                "COVERAGE-FAULT", 0.0, "COVERAGE-FMEA", 0.0,
                "COVERAGE-REQUIREMENT", 0.0, "COVERAGE-INTERFACE", 0.0,
                "COVERAGE-SIGNAL", 0.0, "COVERAGE-FUNCTIONAL", 0.0));

        CoverageScore score = CoverageEngine.calculate(zeroCategoryResults);

        assertEquals(0.0, score.getCompositeScore());
        assertTrue(score.getCategoryScores().values().stream().allMatch(value -> value == 0.0));
    }

    @Test
    void returnsOneHundredForFullyCoveredProcedure() {
        Map<String, RuleResult> fullResults = results(Map.of(
                "COVERAGE-FAULT", 100.0, "COVERAGE-FMEA", 100.0,
                "COVERAGE-REQUIREMENT", 100.0, "COVERAGE-INTERFACE", 100.0,
                "COVERAGE-SIGNAL", 100.0, "COVERAGE-FUNCTIONAL", 100.0));

        assertEquals(100.0, CoverageEngine.calculate(fullResults).getCompositeScore());
    }

    private Map<String, RuleResult> results(Map<String, Double> percentages) {
        Map<String, RuleResult> results = new LinkedHashMap<>();
        CoverageEngine.getWeights().keySet().forEach(category ->
                results.put(category, RuleResult.pass("ok", Map.of("percentage", percentages.get(category)))));
        return results;
    }

    private double score(String category) {
        return category.equals("COVERAGE-FAULT") || category.equals("COVERAGE-FMEA")
                || category.equals("COVERAGE-REQUIREMENT") ? 100.0 : 0.0;
    }
}
