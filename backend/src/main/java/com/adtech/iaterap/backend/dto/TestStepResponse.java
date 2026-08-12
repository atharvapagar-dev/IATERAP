package com.adtech.iaterap.backend.dto;

import com.adtech.iaterap.backend.entity.TestStep;

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
