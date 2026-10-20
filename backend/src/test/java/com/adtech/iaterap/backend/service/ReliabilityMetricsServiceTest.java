package com.adtech.iaterap.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.adtech.iaterap.backend.entity.*;
import com.adtech.iaterap.backend.repository.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReliabilityMetricsServiceTest {
    @Mock private EventLogRepository events; @Mock private SyntheticLruInstanceRepository instances;
    @Mock private LruRepository lrus; @Mock private AircraftRepository aircraft;
    private ReliabilityMetricsService service;
    @BeforeEach void setUp() { service = new ReliabilityMetricsService(events, instances, lrus, aircraft); }

    @Test
    void calculatesMtbfMttrAndFailureRateAtEveryRequiredLevel() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 8, 0);
        SyntheticLruInstance instance = new SyntheticLruInstance(100L, 1L, "SYN-1", 1L); id(instance, 10L);
        Lru lru = new Lru("Flight Computer"); id(lru, 1L); Subsystem subsystem = new Subsystem("Flight Controls"); id(subsystem, 20L); lru.setSubsystem(subsystem);
        Aircraft jet = new Aircraft("Aircraft 01", "IATERAP"); id(jet, 100L);
        when(instances.findAll()).thenReturn(List.of(instance)); when(lrus.findAll()).thenReturn(List.of(lru)); when(aircraft.findAll()).thenReturn(List.of(jet));
        when(events.findAll()).thenReturn(List.of(
                event(10L, 1L, "SORTIE_COMPLETED", start), event(10L, 1L, "FAILURE_DETECTED", start.plusMinutes(10)),
                event(10L, 1L, "REPAIR_COMPLETED", start.plusMinutes(40)), event(10L, 1L, "SORTIE_COMPLETED", start.plusHours(1))));

        var response = service.metrics();

        assertMetric(response.lrus().getFirst()); assertMetric(response.subsystems().getFirst()); assertMetric(response.aircraft().getFirst());
    }

    @Test
    void leavesRateAndMtbfUnavailableWithoutOperatingHours() {
        SyntheticLruInstance instance = new SyntheticLruInstance(100L, 1L, "SYN-1", 1L); id(instance, 10L);
        Lru lru = new Lru("Flight Computer"); id(lru, 1L); when(instances.findAll()).thenReturn(List.of(instance)); when(lrus.findAll()).thenReturn(List.of(lru)); when(aircraft.findAll()).thenReturn(List.of());
        when(events.findAll()).thenReturn(List.of(event(10L, 1L, "FAILURE_DETECTED", LocalDateTime.now())));

        var metric = service.metrics().lrus().getFirst();
        assertNull(metric.mtbfHours()); assertNull(metric.failureRatePerHour());
    }

    @Test
    void usesHandComputedZeroFailureRateWhenOperatingHoursExist() {
        LocalDateTime at = LocalDateTime.of(2026, 1, 1, 8, 0);
        SyntheticLruInstance instance = new SyntheticLruInstance(100L, 1L, "SYN-1", 1L); id(instance, 10L);
        Lru lru = new Lru("Flight Computer"); id(lru, 1L);
        when(instances.findAll()).thenReturn(List.of(instance)); when(lrus.findAll()).thenReturn(List.of(lru)); when(aircraft.findAll()).thenReturn(List.of());
        when(events.findAll()).thenReturn(List.of(event(10L, 1L, "SORTIE_COMPLETED", at), event(10L, 1L, "SORTIE_COMPLETED", at.plusHours(1))));
        var metric = service.metrics().lrus().getFirst();
        assertEquals(2D, metric.operatingHours()); assertNull(metric.mtbfHours()); assertEquals(0D, metric.failureRatePerHour());
    }

    @Test
    void calculatesExactMtbfAsTotalOperatingHoursDividedByFailures() {
        LocalDateTime at = LocalDateTime.of(2026, 1, 1, 8, 0);
        SyntheticLruInstance instance = new SyntheticLruInstance(100L, 1L, "SYN-1", 1L); id(instance, 10L);
        Lru lru = new Lru("Flight Computer"); id(lru, 1L);
        List<EventLog> evidence = new ArrayList<>();
        for (int hour = 0; hour < 12; hour++) evidence.add(event(10L, 1L, "SORTIE_COMPLETED", at.plusHours(hour)));
        for (int failure = 0; failure < 3; failure++) { evidence.add(event(10L, 1L, "FAILURE_DETECTED", at.plusHours(failure * 4L).plusMinutes(5))); evidence.add(event(10L, 1L, "REPAIR_COMPLETED", at.plusHours(failure * 4L).plusMinutes(35))); }
        when(instances.findAll()).thenReturn(List.of(instance)); when(lrus.findAll()).thenReturn(List.of(lru)); when(aircraft.findAll()).thenReturn(List.of()); when(events.findAll()).thenReturn(evidence);
        var metric = service.metrics().lrus().getFirst();
        assertEquals(12D, metric.operatingHours()); assertEquals(3L, metric.failures()); assertEquals(4D, metric.mtbfHours());
    }

    private static void assertMetric(com.adtech.iaterap.backend.dto.ReliabilityMetricsResponse.MetricSummary metric) { assertEquals(2D, metric.operatingHours()); assertEquals(1L, metric.failures()); assertEquals(1L, metric.repairs()); assertEquals(2D, metric.mtbfHours()); assertEquals(0.5D, metric.mttrHours()); assertEquals(0.5D, metric.failureRatePerHour()); }
    private static EventLog event(Long instanceId, Long lruId, String type, LocalDateTime at) { return new EventLog(instanceId, lruId, 1L, type, "INFO", at, "{}", "test"); }
    private static void id(Object entity, Long value) { ReflectionTestUtils.setField(entity, "id", value); }
}
