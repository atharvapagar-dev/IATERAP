package com.adtech.iaterap.backend.dto;

/** Summary of an idempotent synthetic fleet-event generation request. */
public record SyntheticEventGenerationResponse(
        long aircraftId,
        int validationRunsConsidered,
        int syntheticInstancesCreated,
        int eventsCreated,
        int eventsAlreadyPresent) {
}
