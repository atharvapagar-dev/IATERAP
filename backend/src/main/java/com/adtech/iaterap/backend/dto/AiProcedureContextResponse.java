package com.adtech.iaterap.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * Versioned, read-only context contract consumed by the AI Advisory Service.
 * It contains a point-in-time view of a procedure and deterministic coverage
 * evidence; it is never a mutable domain object.
 */
public record AiProcedureContextResponse(
        @JsonProperty("contract_version") String contractVersion,
        @JsonProperty("test_procedure_id") Long testProcedureId,
        String name,
        String description,
        @JsonProperty("lru_id") Long lruId,
        @JsonProperty("approval_status") String approvalStatus,
        @JsonProperty("version_number") Integer versionNumber,
        @JsonProperty("step_count") int stepCount,
        List<AiContextStep> steps,
        @JsonProperty("coverage_context") AiCoverageContext coverageContext,
        @JsonProperty("retrieved_at") Instant retrievedAt
) { }
