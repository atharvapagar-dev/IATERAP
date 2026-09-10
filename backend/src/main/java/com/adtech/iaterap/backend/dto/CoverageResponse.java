package com.adtech.iaterap.backend.dto;

import java.util.Map;

/** Read-only coverage result exposed to the Procedure Builder and demo flow. */
public record CoverageResponse(
        Long procedureId,
        Integer versionNumber,
        Map<String, Double> categories,
        Map<String, Double> weights,
        Double compositeScore,
        Map<String, Boolean> thresholdResults,
        Map<String, String> unavailableReasons
) { }
