package com.adtech.iaterap.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

/** Stable Spring-to-FastAPI request contract for the advisory stub endpoint. */
public record AiSuggestionRequest(
        @JsonProperty("test_procedure_id") Long testProcedureId,
        @JsonProperty("test_step_id") Long testStepId,
        @Size(max = 2000) String prompt) {
}
