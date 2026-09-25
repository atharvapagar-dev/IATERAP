package com.adtech.iaterap.backend.dto;

/** A structured, actionable coverage gap tied to the evaluated LRU. */
public record CoverageGapResponse(String elementType, Long elementId, String description) { }
