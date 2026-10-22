package com.adtech.iaterap.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/** A single reliability formula projected consistently at each supported scope. */
public record ReliabilityMetricSeriesResponse(LocalDateTime generatedAt, String metric,
        List<MetricValue> lrus, List<MetricValue> subsystems, List<MetricValue> aircraft) {
    public record MetricValue(Long id, String name, Double value) { }
}
