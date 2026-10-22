package com.adtech.iaterap.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reliability_report", uniqueConstraints = @UniqueConstraint(columnNames = {"lru_id", "period_start"}))
public class ReliabilityReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "lru_id", nullable = false) private Long lruId;
    @Column(name = "lru_name", nullable = false) private String lruName;
    @Column(name = "period_start", nullable = false) private LocalDate periodStart;
    @Column(name = "period_end", nullable = false) private LocalDate periodEnd;
    @Column(name = "operating_hours", nullable = false) private double operatingHours;
    @Column(nullable = false) private long failures;
    @Column(nullable = false) private long repairs;
    @Column(name = "mtbf_hours") private Double mtbfHours;
    @Column(name = "mttr_hours") private Double mttrHours;
    @Column(name = "failure_rate_per_hour") private Double failureRatePerHour;
    @Column(name = "calculated_at", nullable = false) private LocalDateTime calculatedAt;
    @Column(nullable = false) private String source = "SYNTHETIC";
    protected ReliabilityReport() { }
    public ReliabilityReport(Long lruId, String lruName, LocalDate periodStart, LocalDate periodEnd, double operatingHours, long failures, long repairs, Double mtbfHours, Double mttrHours, Double failureRatePerHour, LocalDateTime calculatedAt) { this.lruId=lruId; this.lruName=lruName; this.periodStart=periodStart; this.periodEnd=periodEnd; this.operatingHours=operatingHours; this.failures=failures; this.repairs=repairs; this.mtbfHours=mtbfHours; this.mttrHours=mttrHours; this.failureRatePerHour=failureRatePerHour; this.calculatedAt=calculatedAt; }
    public Long getId() { return id; } public Long getLruId() { return lruId; } public String getLruName() { return lruName; } public LocalDate getPeriodStart() { return periodStart; } public LocalDate getPeriodEnd() { return periodEnd; } public double getOperatingHours() { return operatingHours; } public long getFailures() { return failures; } public long getRepairs() { return repairs; } public Double getMtbfHours() { return mtbfHours; } public Double getMttrHours() { return mttrHours; } public Double getFailureRatePerHour() { return failureRatePerHour; } public LocalDateTime getCalculatedAt() { return calculatedAt; } public String getSource() { return source; }
}
