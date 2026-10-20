package com.adtech.iaterap.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adtech.iaterap.backend.dto.ReliabilityMetricsResponse;
import com.adtech.iaterap.backend.repository.ReliabilityReportRepository;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReliabilityReportingServiceTest {
    @Mock private ReliabilityMetricsService metrics;
    @Mock private ReliabilityReportRepository reports;

    @Test
    void createsCurrentPeriodProjectionFromHandComputedFormulaValues() {
        ReliabilityMetricsResponse.MetricSummary metric = new ReliabilityMetricsResponse.MetricSummary(7L, "Flight Computer", 12D, 3L, 2L, 4D, 0.5D, 0.25D);
        YearMonth current = YearMonth.now();
        when(metrics.metrics(current.atDay(1), current.atEndOfMonth())).thenReturn(new ReliabilityMetricsResponse(LocalDateTime.of(2026, 10, 20, 10, 0), "synthetic", List.of(metric), List.of(), List.of()));
        when(reports.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ReliabilityReportingService service = new ReliabilityReportingService(metrics, reports);

        var result = service.refreshCurrentPeriod();

        assertEquals(1, result.size());
        assertEquals(12D, result.getFirst().operatingHours());
        assertEquals(4D, result.getFirst().mtbfHours());
        assertEquals(0.5D, result.getFirst().mttrHours());
        assertEquals(0.25D, result.getFirst().failureRatePerHour());
        verify(reports).deleteByPeriodStart(eq(YearMonth.now().atDay(1)));
    }
}
