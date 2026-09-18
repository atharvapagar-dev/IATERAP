package com.adtech.iaterap.backend.coverage;

import com.adtech.iaterap.backend.coverage.rule.RuleCategory;
import com.adtech.iaterap.backend.coverage.rule.RuleResult;
import com.adtech.iaterap.backend.entity.RuleConfiguration;
import com.adtech.iaterap.backend.repository.RuleConfigurationRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CoverageEngineTest {
    private static final List<String> IDS = List.of("COVERAGE-FAULT", "COVERAGE-FMEA", "COVERAGE-REQUIREMENT", "COVERAGE-INTERFACE", "COVERAGE-SIGNAL", "COVERAGE-FUNCTIONAL");

    @Test void calculatesCompositeUsingConfiguredRuntimeWeights() {
        RuleConfigurationRepository repository = mock(RuleConfigurationRepository.class);
        when(repository.findByCategoryAndEnabledTrueOrderByPriorityAscRuleIdAsc(RuleCategory.COVERAGE)).thenReturn(configs("25", "20", "20", "15", "10", "10"));
        CoverageScore score = engine(repository).calculate(results(80, 50, 100, 0, 25, 75));
        assertEquals(60.0, score.getCompositeScore()); // 80*.25 + 50*.20 + 100*.20 + 0*.15 + 25*.10 + 75*.10
        assertTrue(score.getThresholdResults().get("COVERAGE-FAULT"));
    }

    @Test void changingRuntimeWeightsChangesCompositeWithoutCodeChanges() {
        RuleConfigurationRepository repository = mock(RuleConfigurationRepository.class);
        when(repository.findByCategoryAndEnabledTrueOrderByPriorityAscRuleIdAsc(RuleCategory.COVERAGE)).thenReturn(configs("100", "0", "0", "0", "0", "0")).thenReturn(configs("0", "100", "0", "0", "0", "0"));
        assertEquals(80.0, engine(repository).calculate(results(80, 50, 100, 0, 25, 75)).getCompositeScore());
        assertEquals(50.0, engine(repository).calculate(results(80, 50, 100, 0, 25, 75)).getCompositeScore());
    }

    @Test void unavailableActiveCategoryMakesCompositeExplicitlyUnavailable() {
        RuleConfigurationRepository repository = mock(RuleConfigurationRepository.class);
        when(repository.findByCategoryAndEnabledTrueOrderByPriorityAscRuleIdAsc(RuleCategory.COVERAGE)).thenReturn(configs("25", "20", "20", "15", "10", "10"));
        CoverageScore score = engine(repository).calculate(results(100, 100, null, 100, 100, null));
        assertNull(score.getCompositeScore());
        assertEquals("No catalog", score.getUnavailableReasons().get("COVERAGE-REQUIREMENT"));
    }

    @Test void rejectsInvalidConfiguredWeightTotal() {
        RuleConfigurationRepository repository = mock(RuleConfigurationRepository.class);
        when(repository.findByCategoryAndEnabledTrueOrderByPriorityAscRuleIdAsc(RuleCategory.COVERAGE)).thenReturn(configs("25", "20", "20", "15", "10", "9"));
        assertThrows(IllegalStateException.class, () -> engine(repository).calculate(results(0, 0, 0, 0, 0, 0)));
    }

    @Test
    void workedExampleOne_allCategoriesFullyCovered() {
        assertEquals(100.0, configuredEngine().calculate(results(100, 100, 100, 100, 100, 100)).getCompositeScore());
    }

    @Test
    void workedExampleTwo_mixedCoverageProducesHandComputedComposite() {
        // 80*.25 + 60*.20 + 40*.20 + 20*.15 + 0*.10 + 100*.10 = 53
        assertEquals(53.0, configuredEngine().calculate(results(80, 60, 40, 20, 0, 100)).getCompositeScore());
    }

    @Test
    void workedExampleThree_handlesPartiallyCoveredAndUncoveredCategories() {
        // 0*.25 + 0*.20 + 100*.20 + 100*.15 + 50*.10 + 50*.10 = 45
        assertEquals(45.0, configuredEngine().calculate(results(0, 0, 100, 100, 50, 50)).getCompositeScore());
    }

    private CoverageEngine configuredEngine() {
        RuleConfigurationRepository repository = mock(RuleConfigurationRepository.class);
        when(repository.findByCategoryAndEnabledTrueOrderByPriorityAscRuleIdAsc(RuleCategory.COVERAGE))
                .thenReturn(configs("25", "20", "20", "15", "10", "10"));
        return engine(repository);
    }
    private CoverageEngine engine(RuleConfigurationRepository repository) { return new CoverageEngine(null, new CoverageScoringConfigurationService(repository)); }
    private List<RuleConfiguration> configs(String... weights) { return java.util.stream.IntStream.range(0, IDS.size()).mapToObj(i -> { RuleConfiguration rule = new RuleConfiguration(IDS.get(i), RuleCategory.COVERAGE, i, IDS.get(i), true); rule.setWeight(new BigDecimal(weights[i])); rule.setThreshold(BigDecimal.ZERO); return rule; }).toList(); }
    private Map<String, RuleResult> results(Integer... percentages) { Map<String, RuleResult> results = new LinkedHashMap<>(); for (int i = 0; i < IDS.size(); i++) { Integer percentage = percentages[i]; results.put(IDS.get(i), RuleResult.pass("ok", percentage == null ? nullDetails() : Map.of("percentage", percentage))); } return results; }
    private Map<String, Object> nullDetails() { Map<String, Object> details = new LinkedHashMap<>(); details.put("percentage", null); details.put("reason", "No catalog"); return details; }
}
