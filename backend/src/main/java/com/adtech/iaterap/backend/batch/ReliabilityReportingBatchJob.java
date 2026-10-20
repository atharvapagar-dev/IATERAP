package com.adtech.iaterap.backend.batch;
import com.adtech.iaterap.backend.service.ReliabilityReportingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
/** Refreshes the synthetic reliability dashboard projection nightly. */
@Component
public class ReliabilityReportingBatchJob {
    private final ReliabilityReportingService reporting;
    public ReliabilityReportingBatchJob(ReliabilityReportingService reporting) { this.reporting = reporting; }
    @Scheduled(cron = "0 15 2 * * *")
    public void refreshReports() { reporting.refreshCurrentPeriod(); }
}
