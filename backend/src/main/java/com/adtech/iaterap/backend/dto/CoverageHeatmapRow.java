package com.adtech.iaterap.backend.dto;

import java.util.Map;

/** One procedure row in the coverage heatmap aggregation. */
public record CoverageHeatmapRow(Long procedureId, String procedureName, String lruName, String subsystemName,
                                 Map<String, Double> categories, Double compositeScore) { }
