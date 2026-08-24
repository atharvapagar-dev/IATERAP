package com.adtech.iaterap.backend.dto;

import java.util.List;

/**
 * AI-182 / T7-B1: the comparison result for one matched Test Step position
 * between two revisions.
 *
 * <p>Steps are matched by their persisted TestStep id (the same id carried
 * inside each revision's {@code stepsSnapshot}), not by list position, so
 * that reordering a step between revisions is reported as a {@code CHANGED}
 * step (its {@code executionOrder} field differs) rather than as a
 * spurious add/remove pair.</p>
 *
 * <ul>
 *   <li>{@code ADDED} - the step id only exists in the "to" revision. {@code fromStep} is null.</li>
 *   <li>{@code REMOVED} - the step id only exists in the "from" revision. {@code toStep} is null.</li>
 *   <li>{@code CHANGED} - the step id exists in both revisions and at least one field differs.</li>
 *   <li>{@code UNCHANGED} - the step id exists in both revisions with no field differences.</li>
 * </ul>
 */
public class StepComparisonResponse {

    private ChangeType changeType;
    private TestStepResponse fromStep;
    private TestStepResponse toStep;
    private List<FieldChangeResponse> fieldChanges;

    public StepComparisonResponse() {
    }

    public StepComparisonResponse(
            ChangeType changeType,
            TestStepResponse fromStep,
            TestStepResponse toStep,
            List<FieldChangeResponse> fieldChanges
    ) {
        this.changeType = changeType;
        this.fromStep = fromStep;
        this.toStep = toStep;
        this.fieldChanges = fieldChanges;
    }

    public ChangeType getChangeType() { return changeType; }
    public void setChangeType(ChangeType changeType) { this.changeType = changeType; }

    public TestStepResponse getFromStep() { return fromStep; }
    public void setFromStep(TestStepResponse fromStep) { this.fromStep = fromStep; }

    public TestStepResponse getToStep() { return toStep; }
    public void setToStep(TestStepResponse toStep) { this.toStep = toStep; }

    public List<FieldChangeResponse> getFieldChanges() { return fieldChanges; }
    public void setFieldChanges(List<FieldChangeResponse> fieldChanges) { this.fieldChanges = fieldChanges; }
}
