package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.ReliabilityDashboardResponse;
import com.adtech.iaterap.backend.entity.ProcedureHistory;
import com.adtech.iaterap.backend.entity.ValidationReading;
import com.adtech.iaterap.backend.entity.ValidationRun;
import com.adtech.iaterap.backend.repository.ProcedureHistoryRepository;
import com.adtech.iaterap.backend.repository.ValidationReadingRepository;
import com.adtech.iaterap.backend.repository.ValidationRunRepository;
import com.adtech.iaterap.backend.validation.ValidationResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Produces explainable reliability trends from persisted validation evidence.
 * A finding is a MISMATCH, OUTLIER, or MISSING reading; MATCH readings do not
 * inflate a failure rate. Synthetic and external evidence remain labelled.
 */
@Service
public class ReliabilityAnalyticsService {

    private final ValidationRunRepository runs;
    private final ValidationReadingRepository readings;
    private final ProcedureHistoryRepository histories;

    public ReliabilityAnalyticsService(ValidationRunRepository runs,
                                       ValidationReadingRepository readings,
                                       ProcedureHistoryRepository histories) {
        this.runs = runs;
        this.readings = readings;
        this.histories = histories;
    }

    @Transactional(readOnly = true)
    public ReliabilityDashboardResponse dashboard() {
        List<ValidationRun> allRuns = runs.findAll();
        if (allRuns.isEmpty()) {
            return new ReliabilityDashboardResponse(LocalDateTime.now(), 0, 0, 0, 0D, List.of());
        }

        Map<Long, ProcedureHistory> historyById = histories.findAllById(
                        allRuns.stream().map(ValidationRun::getProcedureHistoryId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(ProcedureHistory::getId, history -> history));
        Map<Long, List<ValidationReading>> readingsByRun = readings
                .findByValidationRunIdIn(allRuns.stream().map(ValidationRun::getId).toList())
                .stream().collect(Collectors.groupingBy(ValidationReading::getValidationRunId));

        Map<Long, LruAccumulator> byLru = new HashMap<>();
        for (ValidationRun run : allRuns) {
            ProcedureHistory history = historyById.get(run.getProcedureHistoryId());
            if (history == null) {
                continue; // A deleted/incomplete history is never presented as trustworthy evidence.
            }
            LruAccumulator accumulator = byLru.computeIfAbsent(history.getLruId(),
                    ignored -> new LruAccumulator(history.getLruId(), history.getLruName()));
            accumulator.add(run, readingsByRun.getOrDefault(run.getId(), List.of()));
        }

        List<ReliabilityDashboardResponse.ReliabilityLruSummary> lrus = byLru.values().stream()
                .map(LruAccumulator::toResponse)
                .sorted(Comparator.comparingLong(ReliabilityDashboardResponse.ReliabilityLruSummary::findings)
                        .reversed()
                        .thenComparing(ReliabilityDashboardResponse.ReliabilityLruSummary::lruName,
                                Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        long totalReadings = lrus.stream().mapToLong(ReliabilityDashboardResponse.ReliabilityLruSummary::totalReadings).sum();
        long findings = lrus.stream().mapToLong(ReliabilityDashboardResponse.ReliabilityLruSummary::findings).sum();
        return new ReliabilityDashboardResponse(LocalDateTime.now(), allRuns.size(), totalReadings, findings,
                rate(findings, totalReadings), lrus);
    }

    private static boolean isFinding(ValidationResult result) {
        return result == ValidationResult.MISMATCH || result == ValidationResult.OUTLIER
                || result == ValidationResult.MISSING;
    }

    private static double rate(long numerator, long denominator) {
        if (denominator == 0) return 0D;
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP).doubleValue();
    }

    private static final class LruAccumulator {
        private final Long lruId;
        private final String lruName;
        private final List<RunEvidence> evidence = new ArrayList<>();
        private final Set<String> sources = new LinkedHashSet<>();
        private long totalReadings;
        private long mismatches;
        private long outliers;
        private long missing;
        private LocalDateTime latestEvidenceAt;

        private LruAccumulator(Long lruId, String lruName) {
            this.lruId = lruId;
            this.lruName = lruName == null || lruName.isBlank() ? "Unnamed LRU" : lruName;
        }

        private void add(ValidationRun run, List<ValidationReading> runReadings) {
            long runFindings = 0;
            for (ValidationReading reading : runReadings) {
                totalReadings++;
                if (reading.getResult() == ValidationResult.MISMATCH) mismatches++;
                if (reading.getResult() == ValidationResult.OUTLIER) outliers++;
                if (reading.getResult() == ValidationResult.MISSING) missing++;
                if (isFinding(reading.getResult())) runFindings++;
            }
            evidence.add(new RunEvidence(run.getUploadedAt(), runReadings.size(), runFindings));
            latestEvidenceAt = latestEvidenceAt == null || run.getUploadedAt().isAfter(latestEvidenceAt)
                    ? run.getUploadedAt() : latestEvidenceAt;
            sources.add(run.getSourceFilename().startsWith("synthetic-") ? "SYNTHETIC" : "EXTERNAL");
        }

        private ReliabilityDashboardResponse.ReliabilityLruSummary toResponse() {
            long findings = mismatches + outliers + missing;
            return new ReliabilityDashboardResponse.ReliabilityLruSummary(lruId, lruName, evidence.size(),
                    totalReadings, mismatches, outliers, missing, findings, rate(findings, totalReadings),
                    trend(), latestEvidenceAt, List.copyOf(sources));
        }

        private String trend() {
            if (evidence.size() < 4) return "INSUFFICIENT_DATA";
            List<RunEvidence> ordered = evidence.stream()
                    .sorted(Comparator.comparing(RunEvidence::uploadedAt)).toList();
            int split = ordered.size() / 2;
            double earlier = evidenceRate(ordered.subList(0, split));
            double recent = evidenceRate(ordered.subList(split, ordered.size()));
            if (recent - earlier >= 5D) return "RISING";
            if (earlier - recent >= 5D) return "FALLING";
            return "STABLE";
        }

        private static double evidenceRate(List<RunEvidence> interval) {
            long readings = interval.stream().mapToLong(RunEvidence::readings).sum();
            long findings = interval.stream().mapToLong(RunEvidence::findings).sum();
            return rate(findings, readings);
        }
    }

    private record RunEvidence(LocalDateTime uploadedAt, long readings, long findings) {
    }
}
