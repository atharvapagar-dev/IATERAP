package com.adtech.iaterap.backend.dto;

import com.adtech.iaterap.backend.entity.TestStep;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Stable, read-only step representation for the AI advisory boundary. */
public record AiContextStep(
        @JsonProperty("test_step_id") Long testStepId,
        @JsonProperty("execution_order") Integer executionOrder,
        @JsonProperty("interface_name") String interfaceName,
        @JsonProperty("connector_name") String connectorName,
        @JsonProperty("signal_library_name") String signalLibraryName,
        @JsonProperty("protocol_tag_name") String protocolTagName,
        @JsonProperty("fault_category_name") String faultCategoryName,
        @JsonProperty("expected_behaviour") String expectedBehaviour,
        @JsonProperty("expected_range") String expectedRange,
        @JsonProperty("expected_unit") String expectedUnit,
        @JsonProperty("acceptance_criteria") String acceptanceCriteria,
        @JsonProperty("pass_fail_criteria") String passFailCriteria,
        @JsonProperty("approval_status") String approvalStatus
) {
    public static AiContextStep fromEntity(TestStep step) {
        return new AiContextStep(
                step.getId(),
                step.getExecutionOrder(),
                step.getInterfaceEntity() == null ? null : step.getInterfaceEntity().getName(),
                step.getConnector() == null ? null : step.getConnector().getName(),
                step.getSignalLibrary() == null ? null : step.getSignalLibrary().getName(),
                step.getProtocolTag() == null ? null : step.getProtocolTag().getName(),
                step.getFaultCategory() == null ? null : step.getFaultCategory().getName(),
                step.getExpectedBehaviour(),
                step.getExpectedRange(),
                step.getExpectedUnit() == null ? null : step.getExpectedUnit().name(),
                step.getAcceptanceCriteria(),
                step.getPassFailCriteria(),
                step.getApprovalStatus()
        );
    }
}
