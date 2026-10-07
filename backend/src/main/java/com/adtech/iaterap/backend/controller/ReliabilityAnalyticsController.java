package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.ReliabilityDashboardResponse;
import com.adtech.iaterap.backend.service.ReliabilityAnalyticsService;
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

    public ReliabilityAnalyticsController(ReliabilityAnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public ReliabilityDashboardResponse dashboard() {
        return service.dashboard();
    }
}
