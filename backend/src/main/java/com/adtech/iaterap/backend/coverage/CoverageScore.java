package com.adtech.iaterap.backend.coverage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** A null composite explicitly indicates at least one active category is unavailable. */
public final class CoverageScore {
    private final Map<String, Double> categoryScores;
    private final Double compositeScore;
    private final Map<String, Boolean> thresholdResults;
    private final Map<String, String> unavailableReasons;
    public CoverageScore(Map<String, Double> categoryScores, Double compositeScore, Map<String, Boolean> thresholdResults, Map<String, String> unavailableReasons) {
        this.categoryScores = Collections.unmodifiableMap(new LinkedHashMap<>(categoryScores));
        this.compositeScore = compositeScore;
        this.thresholdResults = Collections.unmodifiableMap(new LinkedHashMap<>(thresholdResults));
        this.unavailableReasons = Collections.unmodifiableMap(new LinkedHashMap<>(unavailableReasons));
    }
    public Map<String, Double> getCategoryScores() { return categoryScores; }
    public Double getCompositeScore() { return compositeScore; }
    public Map<String, Boolean> getThresholdResults() { return thresholdResults; }
    public Map<String, String> getUnavailableReasons() { return unavailableReasons; }
}
