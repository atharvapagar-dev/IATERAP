package com.adtech.iaterap.backend.dto;

import java.util.List;

/** Coverage status aggregation for the procedures assigned to one subsystem. */
public record CoverageHeatmapRow(
        String subsystemName,
        long coveredCount,
        long partiallyCoveredCount,
        long notCoveredCount,
        List<CoverageHeatmapProcedure> procedures
) {
    public record CoverageHeatmapProcedure(Long procedureId, String procedureName, String lruName, String status) { }
}
