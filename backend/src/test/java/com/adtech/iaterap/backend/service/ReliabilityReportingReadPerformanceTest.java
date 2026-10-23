package com.adtech.iaterap.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.adtech.iaterap.backend.entity.ReliabilityReport;
import com.adtech.iaterap.backend.repository.ReliabilityReportRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Read-side performance contract for a full synthetic-fleet sized report projection. */
@ExtendWith(MockitoExtension.class)
class ReliabilityReportingReadPerformanceTest {
    @Mock private ReliabilityMetricsService metrics;
    @Mock private ReliabilityReportRepository reports;

    @Test
    void mapsTenThousandPersistedReportRowsWithinOneSecond() {
        LocalDate start = YearMonth.now().atDay(1);
        List<ReliabilityReport> rows = new ArrayList<>();
        for (long id = 1; id <= 10_000; id++) rows.add(new ReliabilityReport(id, "SYN-LRU-" + id, start, start.plusMonths(1).minusDays(1), 12D, 1L, 1L, 12D, .5D, .08D, LocalDateTime.now()));
        when(reports.findByPeriodStartOrderByLruNameAsc(start)).thenReturn(rows);
        ReliabilityReportingService service = new ReliabilityReportingService(metrics, reports);

        long started = System.nanoTime();
        var result = service.currentPeriod();
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;

        assertEquals(10_000, result.size());
        assertTrue(elapsedMillis < 1_000, "Projection mapping exceeded 1 second: " + elapsedMillis + "ms");
    }
}
