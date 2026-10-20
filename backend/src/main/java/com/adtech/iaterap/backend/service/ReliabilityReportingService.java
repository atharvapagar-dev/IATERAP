package com.adtech.iaterap.backend.service;
import com.adtech.iaterap.backend.dto.ReliabilityMetricsResponse;
import com.adtech.iaterap.backend.dto.ReliabilityReportResponse;
import com.adtech.iaterap.backend.entity.ReliabilityReport;
import com.adtech.iaterap.backend.repository.ReliabilityReportRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReliabilityReportingService {
    private final ReliabilityMetricsService metrics; private final ReliabilityReportRepository reports;
    public ReliabilityReportingService(ReliabilityMetricsService metrics, ReliabilityReportRepository reports) { this.metrics=metrics; this.reports=reports; }
    @Transactional
    public List<ReliabilityReportResponse> refreshCurrentPeriod() {
        YearMonth period = YearMonth.now(); LocalDate start = period.atDay(1); LocalDate end = period.atEndOfMonth();
        ReliabilityMetricsResponse computed = metrics.metrics(start, end); reports.deleteByPeriodStart(start);
        List<ReliabilityReport> saved = reports.saveAll(computed.lrus().stream().map(metric -> new ReliabilityReport(metric.id(), metric.name(), start, end, metric.operatingHours(), metric.failures(), metric.repairs(), metric.mtbfHours(), metric.mttrHours(), metric.failureRatePerHour(), computed.generatedAt())).toList());
        return saved.stream().map(ReliabilityReportResponse::from).toList();
    }
    @Transactional(readOnly = true)
    public List<ReliabilityReportResponse> currentPeriod() { return reports.findByPeriodStartOrderByLruNameAsc(YearMonth.now().atDay(1)).stream().map(ReliabilityReportResponse::from).toList(); }
}
