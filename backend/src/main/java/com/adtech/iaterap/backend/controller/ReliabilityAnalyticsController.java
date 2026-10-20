package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.ReliabilityDashboardResponse;
import com.adtech.iaterap.backend.dto.ReliabilityMetricsResponse;
import com.adtech.iaterap.backend.dto.ReliabilityReportResponse;
import com.adtech.iaterap.backend.service.ReliabilityAnalyticsService;
import com.adtech.iaterap.backend.service.ReliabilityMetricsService;
import com.adtech.iaterap.backend.service.ReliabilityReportingService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only reliability view; calculations are derived from stored evidence only. */
@RestController
@RequestMapping("/api/reliability")
@PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ENGINEER', 'RELIABILITY_ENGINEER')")
public class ReliabilityAnalyticsController {
    private final ReliabilityAnalyticsService service;
    private final ReliabilityMetricsService metricsService;
    private final ReliabilityReportingService reportingService;

    public ReliabilityAnalyticsController(ReliabilityAnalyticsService service, ReliabilityMetricsService metricsService, ReliabilityReportingService reportingService) {
        this.service = service;
        this.metricsService = metricsService;
        this.reportingService = reportingService;
    }

    @GetMapping("/dashboard")
    public ReliabilityDashboardResponse dashboard() {
        return service.dashboard();
    }

    @GetMapping("/metrics")
    public ReliabilityMetricsResponse metrics() { return metricsService.metrics(); }
    @GetMapping("/reports/current")
    public List<ReliabilityReportResponse> currentReports() { return reportingService.currentPeriod(); }
}
