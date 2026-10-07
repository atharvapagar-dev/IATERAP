package com.adtech.iaterap.backend.dto;

import com.adtech.iaterap.backend.entity.ValidationReading;
import com.adtech.iaterap.backend.entity.ValidationRun;
import com.adtech.iaterap.backend.entity.ProcedureHistory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Consolidated report response providing complete outcomes against injected or uploaded
 * validation evidence for a Validation Engineer (T13-B2).
 */
public record ValidationReportResponse(
        Long runId,
        Long procedureHistoryId,
        String procedureName,
        Integer procedureVersionNumber,
        Long lruId,
        String lruName,
        String sourceFilename,
        String sourceFormat,
        String recordedBy,
        String status,
        LocalDateTime uploadedAt,
        boolean synthetic,
        int totalReadings,
        int matchedCount,
        int mismatchedCount,
        int outlierCount,
        int missingCount,
        BigDecimal passRatePercentage,
        List<ValidationReading> readings
) {
    public static ValidationReportResponse of(
            ValidationRun run,
            ProcedureHistory history,
            List<ValidationReading> readings
    ) {
        BigDecimal passRate = BigDecimal.ZERO;
        if (run.getTotalReadings() > 0) {
            passRate = BigDecimal.valueOf(run.getMatchedCount())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(run.getTotalReadings()), 2, RoundingMode.HALF_UP);
        }

        boolean isSynthetic = run.getSourceFilename() != null
                && run.getSourceFilename().toLowerCase().startsWith("synthetic-");

        return new ValidationReportResponse(
                run.getId(),
                run.getProcedureHistoryId(),
                history != null ? history.getName() : null,
                history != null ? history.getVersionNumber() : null,
                history != null ? history.getLruId() : null,
                history != null ? history.getLruName() : null,
                run.getSourceFilename(),
                run.getSourceFormat(),
                run.getRecordedBy(),
                run.getStatus() != null ? run.getStatus().name() : null,
                run.getUploadedAt(),
                isSynthetic,
                run.getTotalReadings(),
                run.getMatchedCount(),
                run.getMismatchedCount(),
                run.getOutlierCount(),
                run.getMissingCount(),
                passRate,
                readings
        );
    }
}
