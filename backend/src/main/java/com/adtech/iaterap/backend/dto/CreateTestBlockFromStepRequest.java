package com.adtech.iaterap.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for promoting an existing TestStep into the shared Test Block
 * library (AI-179 "create-from-step"). Only block-level metadata is
 * user-supplied; the reusable content fields are copied directly from
 * the source TestStep so the block always starts as an exact snapshot
 * of the step it was promoted from.
 */
public class CreateTestBlockFromStepRequest {

    @NotBlank
    private String name;

    private String description;

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
}