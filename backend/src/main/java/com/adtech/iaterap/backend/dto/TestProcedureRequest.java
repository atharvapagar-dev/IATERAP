package com.adtech.iaterap.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Create/update payload for a TestProcedure, including its full ordered
 * list of TestSteps. The Procedure Builder collects the whole procedure
 * client-side and PUTs it in one call; the backend computes the add /
 * edit / reorder / remove diff and, on a content change, writes a new
 * procedure_history revision atomically with the save (PART AD).
 */
public class TestProcedureRequest {

    @NotNull
    private Long lruId;

    @NotBlank
    private String name;

    private String description;

    @NotEmpty
    @Valid
    private List<TestStepRequest> steps;

    public Long getLruId() { return lruId; }
    public void setLruId(Long v) { this.lruId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public List<TestStepRequest> getSteps() { return steps; }
    public void setSteps(List<TestStepRequest> v) { this.steps = v; }
}
