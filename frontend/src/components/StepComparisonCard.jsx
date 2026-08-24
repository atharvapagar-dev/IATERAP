import VersionDiffRow from "./VersionDiffRow";

/**
 * Renders one Test Step's comparison result (from the Version Comparison
 * engine, AI-182 / T7-B1) as a card of Version Diff Rows (AI-183 / T7-B2).
 * A thin composition layer - all actual diff rendering is delegated to
 * VersionDiffRow so the reusable component stays the single source of
 * truth for how a field difference looks.
 */
export default function StepComparisonCard({ step }) {
    const referenceStep = step.toStep || step.fromStep;
    const label = referenceStep
        ? `Step ${referenceStep.executionOrder} - ${referenceStep.expectedBehaviour}`
        : "Step";

    if (step.changeType === "ADDED") {
        return (
            <div className="page-card diff-step diff-step--added">
                <div className="page-toolbar">
                    <strong>{label}</strong>
                    <span className="status-pill diff-row__badge">Added</span>
                </div>
                <p className="helper-text">This Test Step did not exist in the earlier revision.</p>
            </div>
        );
    }

    if (step.changeType === "REMOVED") {
        return (
            <div className="page-card diff-step diff-step--removed">
                <div className="page-toolbar">
                    <strong>{label}</strong>
                    <span className="status-pill diff-row__badge">Removed</span>
                </div>
                <p className="helper-text">This Test Step was removed in the later revision.</p>
            </div>
        );
    }

    return (
        <div className="page-card diff-step">
            <div className="page-toolbar">
                <strong>{label}</strong>
                <span className="status-pill diff-row__badge">
                    {step.changeType === "CHANGED" ? "Changed" : "Unchanged"}
                </span>
            </div>

            {step.fieldChanges.length === 0 ? (
                <p className="helper-text">No field-level differences between these revisions.</p>
            ) : (
                <div className="stack">
                    {step.fieldChanges.map((field) => (
                        <VersionDiffRow
                            key={field.fieldName}
                            fieldName={field.fieldName}
                            oldValue={field.oldValue}
                            newValue={field.newValue}
                            changeType={field.changeType}
                        />
                    ))}
                </div>
            )}
        </div>
    );
}
