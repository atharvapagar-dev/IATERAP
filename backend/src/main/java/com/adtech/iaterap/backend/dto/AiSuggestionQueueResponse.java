package com.adtech.iaterap.backend.dto;
import com.adtech.iaterap.backend.entity.AiSuggestionQueueItem;
import java.time.LocalDateTime;
public record AiSuggestionQueueResponse(Long id, Long testProcedureId, Long testStepId, String suggestion, double confidence, String source, String status, String requestedBy, String decidedBy, LocalDateTime createdAt, LocalDateTime decidedAt) {
    public static AiSuggestionQueueResponse from(AiSuggestionQueueItem item) {
        return new AiSuggestionQueueResponse(item.getId(), item.getTestProcedureId(), item.getTestStepId(), item.getSuggestionText(), item.getConfidence(), item.getSource(), item.getStatus().name(), item.getRequestedBy(), item.getDecidedBy(), item.getCreatedAt(), item.getDecidedAt());
    }
}
