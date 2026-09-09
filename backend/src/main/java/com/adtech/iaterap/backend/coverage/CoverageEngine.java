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

/** Coordinates enabled category rules with live persisted scoring configuration. */
@Service
public class CoverageEngine {
    private final RuleEngine ruleEngine;
    private final CoverageScoringConfigurationService scoringConfigurationService;
    public CoverageEngine(RuleEngine ruleEngine, CoverageScoringConfigurationService scoringConfigurationService) {
        this.ruleEngine = ruleEngine;
        this.scoringConfigurationService = scoringConfigurationService;
    }
    public CoverageScore evaluate(TestProcedure procedure, Lru lru, List<TestStep> steps) {
        return calculate(ruleEngine.evaluate(procedure, lru, steps));
    }
    public CoverageScore calculate(Map<String, RuleResult> results) {
        Map<String, Double> scores = new LinkedHashMap<>();
        Map<String, Boolean> thresholds = new LinkedHashMap<>();
        Map<String, String> unavailable = new LinkedHashMap<>();
        BigDecimal composite = BigDecimal.ZERO;
        boolean complete = true;
        for (CoverageScoringConfigurationService.ScoringRule config : scoringConfigurationService.activeCoverageRules()) {
            RuleResult result = results == null ? null : results.get(config.ruleId());
            if (result == null) throw new IllegalStateException("Missing enabled coverage rule result: " + config.ruleId());
            Object percentage = result.getDetails().get("percentage");
            if (!(percentage instanceof Number number)) {
                complete = false;
                scores.put(config.ruleId(), null);
                thresholds.put(config.ruleId(), null);
                unavailable.put(config.ruleId(), String.valueOf(result.getDetails().get("reason")));
                continue;
            }
            BigDecimal score = new BigDecimal(number.toString()).setScale(2, RoundingMode.HALF_UP);
            scores.put(config.ruleId(), score.doubleValue());
            thresholds.put(config.ruleId(), score.compareTo(config.threshold()) >= 0);
            composite = composite.add(score.multiply(config.weight()).movePointLeft(2));
        }
        return new CoverageScore(scores, complete ? composite.setScale(2, RoundingMode.HALF_UP).doubleValue() : null, thresholds, unavailable);
    }
}
