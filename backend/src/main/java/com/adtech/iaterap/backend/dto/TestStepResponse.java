package com.adtech.iaterap.backend.dto;

import com.adtech.iaterap.backend.entity.TestStep;
import com.fasterxml.jackson.databind.JsonNode;

public class TestStepResponse {

    private Long id;
    private Long interfaceId;
    private String interfaceName;
    private Long connectorId;
    private String connectorName;
    private Long signalLibraryId;
    private String signalLibraryName;
    private Long protocolTagId;
    private String protocolTagName;
    private Long faultCategoryId;
    private String faultCategoryName;
    private String expectedBehaviour;
    private String expectedRange;
    private String calibrationRequirement;
    private String safetyNotes;
    private String acceptanceCriteria;
    private String preconditions;
    private String dependencies;
    private Integer executionOrder;
    private String postConditions;
    private String passFailCriteria;
    private String expectedObservation;
    private String engineerRemarks;
    private String approvalStatus;
    private Integer versionNumber;

    public static TestStepResponse fromEntity(TestStep s) {
        TestStepResponse r = new TestStepResponse();
        r.id = s.getId();
        if (s.getInterfaceEntity() != null) {
            r.interfaceId = s.getInterfaceEntity().getId();
            r.interfaceName = s.getInterfaceEntity().getName();
        }
        if (s.getConnector() != null) {
            r.connectorId = s.getConnector().getId();
            r.connectorName = s.getConnector().getName();
        }
        if (s.getSignalLibrary() != null) {
            r.signalLibraryId = s.getSignalLibrary().getId();
            r.signalLibraryName = s.getSignalLibrary().getName();
        }
        if (s.getProtocolTag() != null) {
            r.protocolTagId = s.getProtocolTag().getId();
            r.protocolTagName = s.getProtocolTag().getName();
        }
        if (s.getFaultCategory() != null) {
            r.faultCategoryId = s.getFaultCategory().getId();
            r.faultCategoryName = s.getFaultCategory().getName();
        }
        r.expectedBehaviour = s.getExpectedBehaviour();
        r.expectedRange = s.getExpectedRange();
        r.calibrationRequirement = s.getCalibrationRequirement();
        r.safetyNotes = s.getSafetyNotes();
        r.acceptanceCriteria = s.getAcceptanceCriteria();
        r.preconditions = s.getPreconditions();
        r.dependencies = s.getDependencies();
        r.executionOrder = s.getExecutionOrder();
        r.postConditions = s.getPostConditions();
        r.passFailCriteria = s.getPassFailCriteria();
        r.expectedObservation = s.getExpectedObservation();
        r.engineerRemarks = s.getEngineerRemarks();
        r.approvalStatus = s.getApprovalStatus();
        r.versionNumber = s.getVersionNumber();
        return r;
    }

    /**
     * AI-182 / T7-B1: reconstructs a TestStepResponse from one element of a
     * {@code ProcedureHistory.stepsSnapshot} JSON array (itself originally
     * written by serializing a {@code List<TestStepResponse>} at save
     * time - see {@code TestProcedureService#serializeSteps}).
     *
     * <p>This class intentionally exposes no setters (it is a read-only
     * response DTO), so it cannot be deserialized via standard Jackson bean
     * binding; this factory mirrors {@link #fromEntity(TestStep)} to
     * rebuild an instance directly from the parsed JSON tree instead.</p>
     */
    public static TestStepResponse fromSnapshot(JsonNode node) {
        TestStepResponse r = new TestStepResponse();
        r.id = longOrNull(node, "id");
        r.interfaceId = longOrNull(node, "interfaceId");
        r.interfaceName = textOrNull(node, "interfaceName");
        r.connectorId = longOrNull(node, "connectorId");
        r.connectorName = textOrNull(node, "connectorName");
        r.signalLibraryId = longOrNull(node, "signalLibraryId");
        r.signalLibraryName = textOrNull(node, "signalLibraryName");
        r.protocolTagId = longOrNull(node, "protocolTagId");
        r.protocolTagName = textOrNull(node, "protocolTagName");
        r.faultCategoryId = longOrNull(node, "faultCategoryId");
        r.faultCategoryName = textOrNull(node, "faultCategoryName");
        r.expectedBehaviour = textOrNull(node, "expectedBehaviour");
        r.expectedRange = textOrNull(node, "expectedRange");
        r.calibrationRequirement = textOrNull(node, "calibrationRequirement");
        r.safetyNotes = textOrNull(node, "safetyNotes");
        r.acceptanceCriteria = textOrNull(node, "acceptanceCriteria");
        r.preconditions = textOrNull(node, "preconditions");
        r.dependencies = textOrNull(node, "dependencies");
        r.executionOrder = intOrNull(node, "executionOrder");
        r.postConditions = textOrNull(node, "postConditions");
        r.passFailCriteria = textOrNull(node, "passFailCriteria");
        r.expectedObservation = textOrNull(node, "expectedObservation");
        r.engineerRemarks = textOrNull(node, "engineerRemarks");
        r.approvalStatus = textOrNull(node, "approvalStatus");
        r.versionNumber = intOrNull(node, "versionNumber");
        return r;
    }

    private static Long longOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asLong();
    }

    private static Integer intOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asInt();
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    public Long getId() { return id; }
    public Long getInterfaceId() { return interfaceId; }
    public String getInterfaceName() { return interfaceName; }
    public Long getConnectorId() { return connectorId; }
    public String getConnectorName() { return connectorName; }
    public Long getSignalLibraryId() { return signalLibraryId; }
    public String getSignalLibraryName() { return signalLibraryName; }
    public Long getProtocolTagId() { return protocolTagId; }
    public String getProtocolTagName() { return protocolTagName; }
    public Long getFaultCategoryId() { return faultCategoryId; }
    public String getFaultCategoryName() { return faultCategoryName; }
    public String getExpectedBehaviour() { return expectedBehaviour; }
    public String getExpectedRange() { return expectedRange; }
    public String getCalibrationRequirement() { return calibrationRequirement; }
    public String getSafetyNotes() { return safetyNotes; }
    public String getAcceptanceCriteria() { return acceptanceCriteria; }
    public String getPreconditions() { return preconditions; }
    public String getDependencies() { return dependencies; }
    public Integer getExecutionOrder() { return executionOrder; }
    public String getPostConditions() { return postConditions; }
    public String getPassFailCriteria() { return passFailCriteria; }
    public String getExpectedObservation() { return expectedObservation; }
    public String getEngineerRemarks() { return engineerRemarks; }
    public String getApprovalStatus() { return approvalStatus; }
    public Integer getVersionNumber() { return versionNumber; }
}
