package com.adtech.iaterap.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Derived reliability formulas over explicitly synthetic event evidence. */
public record ReliabilityMetricsResponse(LocalDateTime generatedAt, String operatingHoursMethod,
        List<MetricSummary> lrus, List<MetricSummary> subsystems, List<MetricSummary> aircraft) {
    public record MetricSummary(Long id, String name, double operatingHours, long failures, long repairs,
            Double mtbfHours, Double mttrHours, Double failureRatePerHour) { }
}
