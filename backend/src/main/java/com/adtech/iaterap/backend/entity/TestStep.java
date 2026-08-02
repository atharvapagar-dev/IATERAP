package com.adtech.iaterap.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_steps")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TestStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "interface_id")
    private Interface interfaceEntity;

    @ManyToOne
    @JoinColumn(name = "connector_id")
    private Connector connector;

    @ManyToOne
    @JoinColumn(name = "signal_library_id")
    private SignalLibrary signalLibrary;

    @ManyToOne
    @JoinColumn(name = "protocol_tag_id")
    private ProtocolTag protocolTag;

    @ManyToOne
    @JoinColumn(name = "fault_template_id")
    private FaultTemplate faultCategory;

    @NotBlank
    @Column(name = "expected_behaviour", length = 1000, nullable = false)
    private String expectedBehaviour;

    @Column(name = "expected_range")
    private String expectedRange;

    @Column(name = "calibration_requirement", length = 500)
    private String calibrationRequirement;

    @Column(name = "safety_notes", length = 1000)
    private String safetyNotes;

    @NotBlank
    @Column(name = "acceptance_criteria", length = 1000, nullable = false)
    private String acceptanceCriteria;

    @Column(length = 1000)
    private String preconditions;

    @Column(length = 1000)
    private String dependencies;

    @NotNull
    @Column(name = "execution_order", nullable = false)
    private Integer executionOrder;

    @Column(name = "post_conditions", length = 1000)
    private String postConditions;

    @NotBlank
    @Column(name = "pass_fail_criteria", length = 1000, nullable = false)
    private String passFailCriteria;

    @Column(name = "expected_observation", length = 1000)
    private String expectedObservation;

    @Column(name = "engineer_remarks", length = 1000)
    private String engineerRemarks;

    @NotBlank
    @Column(name = "approval_status", nullable = false)
    private String approvalStatus = "DRAFT";

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public TestStep() {}

    public Long getId() { return id; }
    public Interface getInterfaceEntity() { return interfaceEntity; }
    public void setInterfaceEntity(Interface interfaceEntity) { this.interfaceEntity = interfaceEntity; }
    public Connector getConnector() { return connector; }
    public void setConnector(Connector connector) { this.connector = connector; }
    public SignalLibrary getSignalLibrary() { return signalLibrary; }
    public void setSignalLibrary(SignalLibrary signalLibrary) { this.signalLibrary = signalLibrary; }
    public ProtocolTag getProtocolTag() { return protocolTag; }
    public void setProtocolTag(ProtocolTag protocolTag) { this.protocolTag = protocolTag; }
    public FaultTemplate getFaultCategory() { return faultCategory; }
    public void setFaultCategory(FaultTemplate faultCategory) { this.faultCategory = faultCategory; }
    public String getExpectedBehaviour() { return expectedBehaviour; }
    public void setExpectedBehaviour(String v) { this.expectedBehaviour = v; }
    public String getExpectedRange() { return expectedRange; }
    public void setExpectedRange(String v) { this.expectedRange = v; }
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
    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String v) { this.approvalStatus = v; }
    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer v) { this.versionNumber = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}