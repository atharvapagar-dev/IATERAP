package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.ReliabilityDashboardResponse;
import com.adtech.iaterap.backend.dto.ReliabilityMetricsResponse;
import com.adtech.iaterap.backend.dto.ReliabilityMetricSeriesResponse;
import com.adtech.iaterap.backend.dto.ReliabilityReportResponse;
import com.adtech.iaterap.backend.dto.ParetoHfraResponse;
import com.adtech.iaterap.backend.dto.ApiResponse;
import com.adtech.iaterap.backend.service.ReliabilityAnalyticsService;
import com.adtech.iaterap.backend.service.ReliabilityMetricsService;
import com.adtech.iaterap.backend.service.ReliabilityReportingService;
import com.adtech.iaterap.backend.service.ParetoHfraService;
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
    private final ParetoHfraService paretoHfraService;

    public ReliabilityAnalyticsController(ReliabilityAnalyticsService service, ReliabilityMetricsService metricsService, ReliabilityReportingService reportingService, ParetoHfraService paretoHfraService) {
        this.service = service;
        this.metricsService = metricsService;
        this.reportingService = reportingService;
        this.paretoHfraService = paretoHfraService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<ReliabilityDashboardResponse> dashboard() {
        return ApiResponse.of(service.dashboard());
    }

    @GetMapping("/metrics")
    public ApiResponse<ReliabilityMetricsResponse> metrics() { return ApiResponse.of(metricsService.metrics()); }
    @GetMapping("/mtbf")
    public ApiResponse<ReliabilityMetricSeriesResponse> mtbf() { return ApiResponse.of(series("MTBF_HOURS", ReliabilityMetricsResponse.MetricSummary::mtbfHours)); }
    @GetMapping("/mttr")
    public ApiResponse<ReliabilityMetricSeriesResponse> mttr() { return ApiResponse.of(series("MTTR_HOURS", ReliabilityMetricsResponse.MetricSummary::mttrHours)); }
    @GetMapping("/failure-rate")
    public ApiResponse<ReliabilityMetricSeriesResponse> failureRate() { return ApiResponse.of(series("FAILURE_RATE_PER_HOUR", ReliabilityMetricsResponse.MetricSummary::failureRatePerHour)); }
    @GetMapping("/reports/current")
    public ApiResponse<List<ReliabilityReportResponse>> currentReports() { return ApiResponse.of(reportingService.currentPeriod()); }
    @GetMapping("/pareto-hfra")
    public ApiResponse<ParetoHfraResponse> paretoHfra() { return ApiResponse.of(paretoHfraService.analyse()); }

    private ReliabilityMetricSeriesResponse series(String metric, java.util.function.Function<ReliabilityMetricsResponse.MetricSummary, Double> extractor) {
        ReliabilityMetricsResponse metrics = metricsService.metrics();
        return new ReliabilityMetricSeriesResponse(metrics.generatedAt(), metric,
                values(metrics.lrus(), extractor), values(metrics.subsystems(), extractor), values(metrics.aircraft(), extractor));
    }
    private List<ReliabilityMetricSeriesResponse.MetricValue> values(List<ReliabilityMetricsResponse.MetricSummary> source, java.util.function.Function<ReliabilityMetricsResponse.MetricSummary, Double> extractor) {
        return source.stream().map(item -> new ReliabilityMetricSeriesResponse.MetricValue(item.id(), item.name(), extractor.apply(item))).toList();
    }
}
