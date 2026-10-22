package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.ParetoHfraResponse;
import com.adtech.iaterap.backend.entity.EventLog;
import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.repository.EventLogRepository;
import com.adtech.iaterap.backend.repository.LruRepository;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParetoHfraService {
    private final EventLogRepository events; private final LruRepository lrus;
    public ParetoHfraService(EventLogRepository events, LruRepository lrus) { this.events=events; this.lrus=lrus; }
    @Transactional(readOnly = true)
    public ParetoHfraResponse analyse() {
        Map<Long, Long> counts = new HashMap<>();
        for (EventLog event : events.findAll()) if ("FAILURE_DETECTED".equals(event.getEventType())) counts.merge(event.getLruId(), 1L, Long::sum);
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        Map<Long, String> names = new HashMap<>(); for (Lru lru : lrus.findAll()) names.put(lru.getId(), lru.getName());
        double mean = counts.isEmpty() ? 0D : total / (double) counts.size();
        double deviation = counts.isEmpty() ? 0D : Math.sqrt(counts.values().stream().mapToDouble(value -> Math.pow(value - mean, 2)).average().orElse(0D));
        double threshold = round(mean + deviation); double[] cumulative = {0D};
        List<ParetoHfraResponse.ParetoItem> items = counts.entrySet().stream().sorted(Map.Entry.<Long, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey())).map(entry -> {
            double contribution = total == 0 ? 0D : round(entry.getValue() * 100D / total); cumulative[0] = round(cumulative[0] + contribution);
            return new ParetoHfraResponse.ParetoItem(entry.getKey(), names.getOrDefault(entry.getKey(), "Unnamed LRU"), entry.getValue(), contribution, cumulative[0], entry.getValue() >= 2, entry.getValue() >= threshold && entry.getValue() >= 2);
        }).toList();
        return new ParetoHfraResponse(LocalDateTime.now(), total, threshold, items);
    }
    private static double round(double value) { return Math.round(value * 100D) / 100D; }
}
