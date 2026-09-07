package com.adtech.iaterap.backend.coverage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable category and weighted composite coverage output. */
public final class CoverageScore {
    private final Map<String, Double> categoryScores;
    private final double compositeScore;

    public CoverageScore(Map<String, Double> categoryScores, double compositeScore) {
        this.categoryScores = Collections.unmodifiableMap(new LinkedHashMap<>(categoryScores));
        this.compositeScore = compositeScore;
    }

    public Map<String, Double> getCategoryScores() { return categoryScores; }
    public double getCompositeScore() { return compositeScore; }
}
