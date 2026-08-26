import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import TestStepCard from "../components/TestStepCard";
import StepComparisonCard from "../components/StepComparisonCard";
import { testProcedureService } from "../services/testProcedureService";
import { testStepService } from "../services/testStepService";
import { versionComparisonService } from "../services/versionComparisonService";

export default function ProcedureBuilderPage() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [procedure, setProcedure] = useState(null);
    const [steps, setSteps] = useState([]);
    const [loading, setLoading] = useState(true);
    const [submittingForReview, setSubmittingForReview] = useState(false);
    const [submitMessage, setSubmitMessage] = useState("");
    const [error, setError] = useState("");

    // AI-182/AI-183: Version Comparison
    const [showCompare, setShowCompare] = useState(false);
    const [history, setHistory] = useState([]);
    const [fromVersion, setFromVersion] = useState("");
    const [toVersion, setToVersion] = useState("");
    const [comparison, setComparison] = useState(null);
    const [comparing, setComparing] = useState(false);
    const [compareError, setCompareError] = useState("");

    // AI-187 / T7-B3: Rollback
    const [rollingBackVersion, setRollingBackVersion] = useState(null);
    const [rollbackError, setRollbackError] = useState("");

    const loadAll = useCallback(async () => {
        try {
            setError("");
            const [procRes, stepsRes] = await Promise.all([
                testProcedureService.getProcedureById(id),
                testProcedureService.getSteps(id),
            ]);
            setProcedure(procRes.data);
            setSteps(stepsRes.data);
        } catch (fetchError) {
            console.error("Failed to load Procedure Builder data:", fetchError);
            setError("Unable to load this Test Procedure right now.");
        } finally {
            setLoading(false);
        }
    }, [id]);

    useEffect(() => {
        void loadAll();
    }, [loadAll]);

    const handleAddStep = async (stepPayload) => {
        await testProcedureService.addStep(id, stepPayload);
        await loadAll();
    };

    const handleUpdateStep = async (stepId, stepPayload) => {
        await testStepService.updateStep(stepId, stepPayload);
        await loadAll();
    };

    const handleDeleteStep = async (stepId) => {
        const isConfirmed = window.confirm("Delete this Test Step? This action cannot be undone.");
        if (!isConfirmed) {
            return;
        }

        await testStepService.deleteStep(stepId);
        await loadAll();
    };

    const handleMove = async (index, direction) => {
        const targetIndex = index + direction;
        if (targetIndex < 0 || targetIndex >= steps.length) {
            return;
        }

        const reordered = [...steps];
        const [moved] = reordered.splice(index, 1);
        reordered.splice(targetIndex, 0, moved);

        try {
            await testProcedureService.reorderSteps(id, reordered.map((s) => s.id));
            await loadAll();
        } catch (reorderError) {
            console.error("Failed to reorder Test Steps:", reorderError);
            setError("Could not reorder Test Steps. Please try again.");
        }
    };

    const handleToggleCompare = async () => {
        const opening = !showCompare;
        setShowCompare(opening);
        if (opening && history.length === 0) {
            try {
                const historyRes = await versionComparisonService.getHistory(id);
                setHistory(historyRes.data);
                if (historyRes.data.length >= 2) {
                    setFromVersion(String(historyRes.data[0].versionNumber));
                    setToVersion(String(historyRes.data[historyRes.data.length - 1].versionNumber));
                }
            } catch (historyError) {
                console.error("Failed to load revision history:", historyError);
                setCompareError("Unable to load revision history for this Test Procedure.");
            }
        }
    };

    const handleCompare = async () => {
        if (!fromVersion || !toVersion) {
            return;
        }
        setComparing(true);
        setCompareError("");
        setComparison(null);
        try {
            const compareRes = await versionComparisonService.compareVersions(id, fromVersion, toVersion);
            setComparison(compareRes.data);
        } catch (compareErrorResponse) {
            console.error("Failed to compare Test Procedure revisions:", compareErrorResponse);
            setCompareError(
                compareErrorResponse?.response?.data?.message ||
                    "Unable to compare these revisions. Please check the version numbers and try again."
            );
        } finally {
            setComparing(false);
        }
    };

    // AI-187 / T7-B3: rolling back never edits or removes the source
    // revision (or any other existing revision) -- it always appends a
    // brand new revision matching the source revision's content.
    const handleRollback = async (versionNumber) => {
        const confirmed = window.confirm(
            `Roll back to Revision ${versionNumber}? This will create a new revision ` +
                `with Revision ${versionNumber}'s content. Revision ${versionNumber} and every ` +
                `other existing revision will be left unchanged.`
        );
        if (!confirmed) {
            return;
        }

        setRollingBackVersion(versionNumber);
        setRollbackError("");
        try {
            await versionComparisonService.rollbackToRevision(id, versionNumber);
            await loadAll();
            const historyRes = await versionComparisonService.getHistory(id);
            setHistory(historyRes.data);
            setComparison(null);
        } catch (rollbackErrorResponse) {
            console.error("Failed to roll back Test Procedure:", rollbackErrorResponse);
            setRollbackError(
                rollbackErrorResponse?.response?.data?.message ||
                    `Unable to roll back to Revision ${versionNumber}. Please try again.`
            );
        } finally {
            setRollingBackVersion(null);
        }
    };

    const handleSubmitForReview = async () => {
        setError("");
        setSubmitMessage("");
        setSubmittingForReview(true);

        try {
            const response = await testProcedureService.submitForReview(id);
            await loadAll();
            setSubmitMessage(`Submitted for review - now at version ${response.data.versionNumber}.`);
        } catch (submitError) {
            console.error("Failed to submit Test Procedure for review:", submitError);
            setError(
                submitError?.response?.data?.message ||
                    "Could not submit this Test Procedure for review. Resolve any validation errors and try again."
            );
        } finally {
            setSubmittingForReview(false);
        }
    };

    if (loading) {
        return (
            <ManagementLayout title="Procedure Builder" description="Loading...">
                <p className="helper-text">Loading Procedure Builder...</p>
            </ManagementLayout>
        );
    }

    if (error || !procedure) {
        return (
            <ManagementLayout title="Procedure Builder" description="">
                <p style={{ color: "var(--danger)" }}>{error || "Test Procedure not found."}</p>
                <button onClick={() => navigate("/procedures")}>Back to Procedures</button>
            </ManagementLayout>
        );
    }

    return (
        <ManagementLayout
            title={`Procedure Builder - ${procedure.name}`}
            description={`LRU: ${procedure.lru?.name || "-"}  |  Status: ${procedure.approvalStatus}  |  Version: ${procedure.versionNumber}`}
        >
            {error ? <p style={{ color: "var(--danger)" }}>{error}</p> : null}
            {submitMessage ? <p className="helper-text">{submitMessage}</p> : null}

            <section className="stack">
                {!steps.length ? (
                    <div className="empty-state">
                        No Test Steps yet. Add the first one below.
                    </div>
                ) : (
                    steps.map((step, index) => (
                        <TestStepCard
                            key={step.id}
                            step={step}
                            lruId={procedure.lru?.id}
                            onSave={(payload) => handleUpdateStep(step.id, payload)}
                            onDelete={handleDeleteStep}
                            onMoveUp={() => handleMove(index, -1)}
                            onMoveDown={() => handleMove(index, 1)}
                            isFirst={index === 0}
                            isLast={index === steps.length - 1}
                        />
                    ))
                )}

                <TestStepCard
                    step={null}
                    lruId={procedure.lru?.id}
                    onSave={handleAddStep}
                />
            </section>

            <section className="stack" style={{ marginTop: "28px" }}>
                <div className="page-toolbar">
                    <h2 className="section-title" style={{ margin: 0 }}>Version Comparison</h2>
                    {procedure.approvalStatus === "DRAFT" ? (
                        <button
                            type="button"
                            onClick={handleSubmitForReview}
                            disabled={submittingForReview}
                        >
                            {submittingForReview ? "Submitting..." : "Submit for Review"}
                        </button>
                    ) : null}
                    <button type="button" className="button--secondary" onClick={handleToggleCompare}>
                        {showCompare ? "Hide" : "Compare Revisions"}
                    </button>
                </div>

                {showCompare && (
                    <div className="page-card">
                        {history.length > 0 && (
                            <div className="stack" style={{ marginBottom: "18px" }}>
                                <h3 className="section-title" style={{ margin: 0 }}>Revision History</h3>
                                <p className="helper-text">
                                    Rolling back creates a new revision matching the selected revision's content.
                                    It never edits or removes any existing revision.
                                </p>
                                <div className="stack">
                                    {history.map((h) => (
                                        <div key={h.versionNumber} className="page-toolbar">
                                            <span>
                                                Revision {h.versionNumber}
                                                {h.versionNumber === procedure.versionNumber ? " (current)" : ""}
                                            </span>
                                            <button
                                                type="button"
                                                className="button--secondary"
                                                disabled={
                                                    rollingBackVersion !== null ||
                                                    h.versionNumber === procedure.versionNumber
                                                }
                                                onClick={() => handleRollback(h.versionNumber)}
                                            >
                                                {rollingBackVersion === h.versionNumber
                                                    ? "Rolling back..."
                                                    : "Rollback to this revision"}
                                            </button>
                                        </div>
                                    ))}
                                </div>
                                {rollbackError ? <p style={{ color: "var(--danger)" }}>{rollbackError}</p> : null}
                            </div>
                        )}

                        {history.length < 2 ? (
                            <p className="helper-text">
                                This Test Procedure needs at least two saved revisions before versions can be compared.
                            </p>
                        ) : (
                            <>
                                <div className="form-grid form-grid--two">
                                    <div className="form-field">
                                        <label>From Revision</label>
                                        <select value={fromVersion} onChange={(e) => setFromVersion(e.target.value)}>
                                            {history.map((h) => (
                                                <option key={h.versionNumber} value={h.versionNumber}>
                                                    Revision {h.versionNumber}
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                    <div className="form-field">
                                        <label>To Revision</label>
                                        <select value={toVersion} onChange={(e) => setToVersion(e.target.value)}>
                                            {history.map((h) => (
                                                <option key={h.versionNumber} value={h.versionNumber}>
                                                    Revision {h.versionNumber}
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                </div>
                                <div className="form-actions">
                                    <button type="button" onClick={handleCompare} disabled={comparing}>
                                        {comparing ? "Comparing..." : "Compare"}
                                    </button>
                                </div>
                            </>
                        )}

                        {compareError ? <p style={{ color: "var(--danger)" }}>{compareError}</p> : null}

                        {comparison && (
                            <div className="stack" style={{ marginTop: "18px" }}>
                                <p className="helper-text">
                                    Revision {comparison.fromVersion} → Revision {comparison.toVersion}
                                    {" · "}
                                    {comparison.addedStepCount} added, {comparison.removedStepCount} removed,{" "}
                                    {comparison.changedStepCount} changed, {comparison.unchangedStepCount} unchanged
                                </p>
                                {comparison.steps.map((step, index) => (
                                    <StepComparisonCard key={step.toStep?.id ?? step.fromStep?.id ?? index} step={step} />
                                ))}
                            </div>
                        )}
                    </div>
                )}
            </section>
        </ManagementLayout>
    );
}
