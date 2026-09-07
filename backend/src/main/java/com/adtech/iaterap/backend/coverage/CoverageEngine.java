package com.adtech.iaterap.backend.coverage;

import com.adtech.iaterap.backend.coverage.rule.RuleEngine;
import com.adtech.iaterap.backend.coverage.rule.RuleResult;
import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.TestProcedure;
import com.adtech.iaterap.backend.entity.TestStep;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Coordinates category rules and calculates the configured composite score. */
@Service
public class CoverageEngine {
    private static final Map<String, Double> WEIGHTS = Map.of(
            "COVERAGE-FAULT", 0.25,
            "COVERAGE-FMEA", 0.20,
            "COVERAGE-REQUIREMENT", 0.20,
            "COVERAGE-INTERFACE", 0.15,
            "COVERAGE-SIGNAL", 0.10,
            "COVERAGE-FUNCTIONAL", 0.10);

    private final RuleEngine ruleEngine;

    public CoverageEngine(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    public CoverageScore evaluate(TestProcedure procedure, Lru lru, List<TestStep> steps) {
        return calculate(ruleEngine.evaluate(procedure, lru, steps));
    }

    /** Calculates a score from category-rule results; exposed for deterministic unit testing. */
    public static CoverageScore calculate(Map<String, RuleResult> results) {
        Map<String, Double> scores = new LinkedHashMap<>();
        double composite = 0.0;
        for (String category : WEIGHTS.keySet()) {
            RuleResult result = results == null ? null : results.get(category);
            if (result == null) {
                throw new IllegalStateException("Missing coverage category result: " + category);
            }
            Object percentage = result.getDetails().get("percentage");
            if (!(percentage instanceof Number number)) {
                throw new IllegalStateException("Coverage result has no percentage: " + category);
            }
            double score = number.doubleValue();
            scores.put(category, round(score));
            composite += score * WEIGHTS.get(category);
        }
        return new CoverageScore(scores, round(composite));
    }

    public static Map<String, Double> getWeights() {
        return WEIGHTS;
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
