package com.adtech.iaterap.backend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Payload for inserting a shared Test Block into a specific TestProcedure
 * (AI-179 "insert-into-procedure"). A block's content is LRU-agnostic by
 * design, so the caller supplies the target procedure's own
 * Interface/Connector/Signal/Protocol/Fault bindings here.
 */
public class InsertTestBlockRequest {

    @NotNull
    private Long testProcedureId;

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

    public Long getTestProcedureId() { return testProcedureId; }
    public void setTestProcedureId(Long v) { this.testProcedureId = v; }
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
}