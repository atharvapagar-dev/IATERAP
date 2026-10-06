package com.adtech.iaterap.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Read-only reliability summary derived from persisted validation evidence. */
public record ReliabilityDashboardResponse(
        LocalDateTime generatedAt,
        long totalRuns,
        long totalReadings,
        long findings,
        double findingRate,
        List<ReliabilityLruSummary> lrus) {

    public record ReliabilityLruSummary(
            Long lruId,
            String lruName,
            long totalRuns,
            long totalReadings,
            long mismatches,
            long outliers,
            long missing,
            long findings,
            double findingRate,
            String trend,
            LocalDateTime latestEvidenceAt,
            List<String> evidenceSources) {
    }
}
