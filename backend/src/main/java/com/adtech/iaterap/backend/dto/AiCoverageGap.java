package com.adtech.iaterap.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** A deterministic coverage gap; no AI inference is performed here. */
public record AiCoverageGap(
        @JsonProperty("element_type") String elementType,
        @JsonProperty("element_id") Long elementId,
        String description
) { }
