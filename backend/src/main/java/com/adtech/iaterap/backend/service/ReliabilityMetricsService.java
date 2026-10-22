package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.ReliabilityMetricsResponse;
import com.adtech.iaterap.backend.entity.*;
import com.adtech.iaterap.backend.repository.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Calculates MTBF, MTTR and failure rate only from persisted synthetic events. */
@Service
public class ReliabilityMetricsService {
    private final EventLogRepository events; private final SyntheticLruInstanceRepository instances;
    private final LruRepository lrus; private final AircraftRepository aircraft;
    public ReliabilityMetricsService(EventLogRepository events, SyntheticLruInstanceRepository instances,
            LruRepository lrus, AircraftRepository aircraft) { this.events = events; this.instances = instances; this.lrus = lrus; this.aircraft = aircraft; }

    @Transactional(readOnly = true)
    public ReliabilityMetricsResponse metrics() {
        return metrics(null, null);
    }

    @Transactional(readOnly = true)
    public ReliabilityMetricsResponse metrics(LocalDate periodStart, LocalDate periodEnd) {
        Map<Long, SyntheticLruInstance> instanceById = new HashMap<>(); instances.findAll().forEach(i -> instanceById.put(i.getId(), i));
        Map<Long, Lru> lruById = new HashMap<>(); lrus.findAll().forEach(l -> lruById.put(l.getId(), l));
        Map<Long, Aircraft> aircraftById = new HashMap<>(); aircraft.findAll().forEach(a -> aircraftById.put(a.getId(), a));
        Map<Long, List<EventLog>> byInstance = new HashMap<>();
        for (EventLog event : events.findAll()) {
            LocalDate eventDate = event.getTimestamp().toLocalDate();
            if ((periodStart == null || !eventDate.isBefore(periodStart)) && (periodEnd == null || !eventDate.isAfter(periodEnd))) {
                byInstance.computeIfAbsent(event.getSyntheticLruInstanceId(), key -> new ArrayList<>()).add(event);
            }
        }
        Map<Long, Accumulator> lruMetrics = new HashMap<>(), subsystemMetrics = new HashMap<>(), aircraftMetrics = new HashMap<>();
        for (var entry : byInstance.entrySet()) {
            SyntheticLruInstance instance = instanceById.get(entry.getKey()); if (instance == null) continue;
            Lru lru = lruById.get(instance.getLruId());
            Measurement measurement = measure(entry.getValue());
            add(lruMetrics, instance.getLruId(), lru == null ? "Unnamed LRU" : lru.getName(), measurement);
            if (lru != null && lru.getSubsystem() != null) add(subsystemMetrics, lru.getSubsystem().getId(), lru.getSubsystem().getName(), measurement);
            Aircraft installedOn = aircraftById.get(instance.getAircraftId());
            if (installedOn != null) add(aircraftMetrics, installedOn.getId(), installedOn.getName(), measurement);
        }
        return new ReliabilityMetricsResponse(LocalDateTime.now(), "Each SORTIE_COMPLETED synthetic event represents one simulated operating hour.", summaries(lruMetrics), summaries(subsystemMetrics), summaries(aircraftMetrics));
    }
    private static void add(Map<Long, Accumulator> values, Long id, String name, Measurement measurement) { values.computeIfAbsent(id, key -> new Accumulator(id, name)).add(measurement); }
    private static List<ReliabilityMetricsResponse.MetricSummary> summaries(Map<Long, Accumulator> values) { return values.values().stream().map(Accumulator::response).sorted(Comparator.comparing(ReliabilityMetricsResponse.MetricSummary::name, Comparator.nullsLast(String::compareToIgnoreCase))).toList(); }
    private static Measurement measure(List<EventLog> events) {
        events.sort(Comparator.comparing(EventLog::getTimestamp)); long sorties = 0, failures = 0, repairs = 0; double repairHours = 0; LocalDateTime openFailure = null;
        for (EventLog event : events) { switch (event.getEventType()) {
            case "SORTIE_COMPLETED" -> sorties++;
            case "FAILURE_DETECTED" -> { failures++; if (openFailure == null) openFailure = event.getTimestamp(); }
            case "REPAIR_COMPLETED" -> { repairs++; if (openFailure != null) { repairHours += Duration.between(openFailure, event.getTimestamp()).toMinutes() / 60D; openFailure = null; } }
            default -> { }
        }} return new Measurement(sorties, failures, repairs, repairHours);
    }
    private record Measurement(double hours, long failures, long repairs, double repairHours) { }
    private static final class Accumulator { final Long id; final String name; double hours, repairHours; long failures, repairs; Accumulator(Long id, String name) { this.id=id; this.name=name; } void add(Measurement m) { hours+=m.hours; failures+=m.failures; repairs+=m.repairs; repairHours+=m.repairHours; } ReliabilityMetricsResponse.MetricSummary response() { return new ReliabilityMetricsResponse.MetricSummary(id,name,hours,failures,repairs, hours == 0 || failures == 0 ? null : round(hours / failures), repairs == 0 ? null : round(repairHours / repairs), hours == 0 ? null : round(failures / hours)); } }
    private static double round(double value) { return Math.round(value * 100D) / 100D; }
}
