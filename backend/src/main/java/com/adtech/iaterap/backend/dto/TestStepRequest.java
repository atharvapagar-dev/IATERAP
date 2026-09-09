package com.adtech.iaterap.backend.dto;

import com.adtech.iaterap.backend.validation.EngineeringUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Input shape for a single TestStep inside a TestProcedure create/update
 * payload (Procedure Builder "Test Step Card"). Mirrors the existing
 * TestStep JPA entity fields; does not invent new ones.
 */
public class TestStepRequest {

    private Long id; // null for a new step; present to update/reorder an existing step

    @NotNull
    private Long interfaceId;

    @NotNull
    private Long connectorId;

    @NotNull
    private Long signalLibraryId;

    @NotNull
    private Long protocolTagId;

    @NotNull
    private Long faultCategoryId;

    @NotBlank
    private String expectedBehaviour;

    private String expectedRange;
    private EngineeringUnit expectedUnit;
    private String calibrationRequirement;
    private String safetyNotes;

    @NotBlank
    private String acceptanceCriteria;

    private String preconditions;
    private String dependencies;

    @NotNull
    private Integer executionOrder;

    private String postConditions;

    @NotBlank
    private String passFailCriteria;

    private String expectedObservation;
    private String engineerRemarks;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getInterfaceId() { return interfaceId; }
    public void setInterfaceId(Long v) { this.interfaceId = v; }
    public Long getConnectorId() { return connectorId; }
    public void setConnectorId(Long v) { this.connectorId = v; }
    public Long getSignalLibraryId() { return signalLibraryId; }
    public void setSignalLibraryId(Long v) { this.signalLibraryId = v; }
    public Long getProtocolTagId() { return protocolTagId; }
    public void setProtocolTagId(Long v) { this.protocolTagId = v; }
    public Long getFaultCategoryId() { return faultCategoryId; }
    public void setFaultCategoryId(Long v) { this.faultCategoryId = v; }
    public String getExpectedBehaviour() { return expectedBehaviour; }
    public void setExpectedBehaviour(String v) { this.expectedBehaviour = v; }
    public String getExpectedRange() { return expectedRange; }
    public void setExpectedRange(String v) { this.expectedRange = v; }
    public EngineeringUnit getExpectedUnit() { return expectedUnit; }
    public void setExpectedUnit(EngineeringUnit v) { this.expectedUnit = v; }
    public String getCalibrationRequirement() { return calibrationRequirement; }
    public void setCalibrationRequirement(String v) { this.calibrationRequirement = v; }
    public String getSafetyNotes() { return safetyNotes; }
    public void setSafetyNotes(String v) { this.safetyNotes = v; }
    public String getAcceptanceCriteria() { return acceptanceCriteria; }
    public void setAcceptanceCriteria(String v) { this.acceptanceCriteria = v; }
    public String getPreconditions() { return preconditions; }
    public void setPreconditions(String v) { this.preconditions = v; }
    public String getDependencies() { return dependencies; }
    public void setDependencies(String v) { this.dependencies = v; }
    public Integer getExecutionOrder() { return executionOrder; }
    public void setExecutionOrder(Integer v) { this.executionOrder = v; }
    public String getPostConditions() { return postConditions; }
    public void setPostConditions(String v) { this.postConditions = v; }
    public String getPassFailCriteria() { return passFailCriteria; }
    public void setPassFailCriteria(String v) { this.passFailCriteria = v; }
    public String getExpectedObservation() { return expectedObservation; }
    public void setExpectedObservation(String v) { this.expectedObservation = v; }
    public String getEngineerRemarks() { return engineerRemarks; }
    public void setEngineerRemarks(String v) { this.engineerRemarks = v; }
}
