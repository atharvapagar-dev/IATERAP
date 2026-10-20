package com.adtech.iaterap.backend.dto;
import com.adtech.iaterap.backend.entity.ReliabilityReport;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record ReliabilityReportResponse(Long lruId, String lruName, LocalDate periodStart, LocalDate periodEnd, double operatingHours, long failures, long repairs, Double mtbfHours, Double mttrHours, Double failureRatePerHour, LocalDateTime calculatedAt, String source) {
    public static ReliabilityReportResponse from(ReliabilityReport report) { return new ReliabilityReportResponse(report.getLruId(), report.getLruName(), report.getPeriodStart(), report.getPeriodEnd(), report.getOperatingHours(), report.getFailures(), report.getRepairs(), report.getMtbfHours(), report.getMttrHours(), report.getFailureRatePerHour(), report.getCalculatedAt(), report.getSource()); }
}
