package com.adtech.iaterap.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/** Read-only live coverage context supplied to the advisory service. */
public record AiCoverageContext(
        Map<String, Double> categories,
        Map<String, Double> weights,
        @JsonProperty("composite_score") Double compositeScore,
        @JsonProperty("threshold_results") Map<String, Boolean> thresholdResults,
        @JsonProperty("unavailable_reasons") Map<String, String> unavailableReasons,
        @JsonProperty("coverage_gaps") List<AiCoverageGap> coverageGaps
) { }
