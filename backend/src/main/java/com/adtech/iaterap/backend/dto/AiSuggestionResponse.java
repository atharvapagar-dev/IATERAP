package com.adtech.iaterap.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

/** Structured advisory response returned by the independent FastAPI service. */
public record AiSuggestionResponse(
        String suggestion,
        double confidence,
        String source,
        @JsonProperty("generated_at") OffsetDateTime generatedAt) {
}
