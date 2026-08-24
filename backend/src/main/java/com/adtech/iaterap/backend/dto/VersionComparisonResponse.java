package com.adtech.iaterap.backend.dto;

import java.util.List;

/**
 * AI-182 / T7-B1: structured, Test-Step-by-Test-Step, field-by-field
 * comparison of two revisions of the same TestProcedure (Sec 19).
 *
 * <p>{@code steps} is ordered top-to-bottom by the effective execution
 * order of each matched position (the "to" revision's order where the
 * step still exists there, otherwise its last known "from" order), so the
 * frontend can render it directly as a top-to-bottom revision diff.</p>
 */
public class VersionComparisonResponse {

    private Long procedureId;
    private String procedureName;
    private Integer fromVersion;
    private Integer toVersion;
    private int addedStepCount;
    private int removedStepCount;
    private int changedStepCount;
    private int unchangedStepCount;
    private List<StepComparisonResponse> steps;

    public VersionComparisonResponse() {
    }

    public Long getProcedureId() { return procedureId; }
    public void setProcedureId(Long procedureId) { this.procedureId = procedureId; }

    public String getProcedureName() { return procedureName; }
    public void setProcedureName(String procedureName) { this.procedureName = procedureName; }

    public Integer getFromVersion() { return fromVersion; }
    public void setFromVersion(Integer fromVersion) { this.fromVersion = fromVersion; }

    public Integer getToVersion() { return toVersion; }
    public void setToVersion(Integer toVersion) { this.toVersion = toVersion; }

    public int getAddedStepCount() { return addedStepCount; }
    public void setAddedStepCount(int addedStepCount) { this.addedStepCount = addedStepCount; }

    public int getRemovedStepCount() { return removedStepCount; }
    public void setRemovedStepCount(int removedStepCount) { this.removedStepCount = removedStepCount; }

    public int getChangedStepCount() { return changedStepCount; }
    public void setChangedStepCount(int changedStepCount) { this.changedStepCount = changedStepCount; }

    public int getUnchangedStepCount() { return unchangedStepCount; }
    public void setUnchangedStepCount(int unchangedStepCount) { this.unchangedStepCount = unchangedStepCount; }

    public List<StepComparisonResponse> getSteps() { return steps; }
    public void setSteps(List<StepComparisonResponse> steps) { this.steps = steps; }
}
