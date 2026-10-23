package com.adtech.iaterap.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Transparent Pareto ranking and repeat-failure flagging from persisted event evidence. */
public record ParetoHfraResponse(LocalDateTime generatedAt, long totalFailures, double hfraThreshold,
        List<ParetoItem> items) {
    public record ParetoItem(Long lruId, String lruName, long failures, double contributionPercent,
            double cumulativePercent, boolean repeatFailure, boolean hfraFlag) { }
}
