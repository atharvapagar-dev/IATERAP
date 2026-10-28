package com.adtech.iaterap.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.adtech.iaterap.backend.dto.ReliabilityMetricsResponse;
import com.adtech.iaterap.backend.entity.ReliabilityReport;
import com.adtech.iaterap.backend.repository.ReliabilityReportRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Timed reporting leg of the end-to-end procedure-to-dashboard regression contract. */
@ExtendWith(MockitoExtension.class)
class ReliabilityPipelineTimingTest {
    @Mock private ReliabilityMetricsService metrics;
    @Mock private ReliabilityReportRepository reports;

    @Test
    void refreshesAndReadsDashboardProjectionWithinOneSecond() {
        var metric = new ReliabilityMetricsResponse.MetricSummary(7L, "Flight Computer", 12D, 3L, 3L, 4D, .5D, .25D);
        when(metrics.metrics(any(), any())).thenReturn(new ReliabilityMetricsResponse(LocalDateTime.now(), "synthetic", List.of(metric), List.of(), List.of()));
        when(reports.saveAll(any())).thenAnswer(call -> call.getArgument(0));
        ReliabilityReportingService service = new ReliabilityReportingService(metrics, reports);

        long started = System.nanoTime();
        var refreshed = service.refreshCurrentPeriod();
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;

        assertEquals(1, refreshed.size());
        assertTrue(elapsedMillis < 1_000, "Reporting refresh exceeded 1 second: " + elapsedMillis + "ms");
    }
}
