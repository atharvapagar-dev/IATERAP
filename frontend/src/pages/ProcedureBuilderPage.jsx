import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import TestStepCard from "../components/TestStepCard";
import StepComparisonCard from "../components/StepComparisonCard";
import { testProcedureService } from "../services/testProcedureService";
import { testBlockService } from "../services/testBlockService";
import { getLrus } from "../services/lruService";
import { interfaceService } from "../services/interfaceService";
import { connectorService } from "../services/connectorService";
import { signalLibraryService } from "../services/signalLibraryService";
import { protocolTagService } from "../services/protocolTagService";
import { faultTemplateService } from "../services/faultTemplateService";
import { versionComparisonService } from "../services/versionComparisonService";
import CoverageSummaryCard from "../components/CoverageSummaryCard";
import api from "../services/api";

let tempKeyCounter = 0;

function nextTempKey() {
    tempKeyCounter += 1;
    return `new-${tempKeyCounter}`;
}

function createDraftSnapshot({ lruId = "", name = "", description = "", steps = [] } = {}) {
    return JSON.stringify({
        lruId: String(lruId),
        name,
        description,
        steps: steps.map((step) => ({
            id: step.id || null,
            interfaceId: step.interfaceId || null,
            connectorId: step.connectorId || null,
            signalLibraryId: step.signalLibraryId || null,
            protocolTagId: step.protocolTagId || null,
            faultCategoryId: step.faultCategoryId || null,
            expectedBehaviour: step.expectedBehaviour || "",
            expectedRange: step.expectedRange || "",
            calibrationRequirement: step.calibrationRequirement || "",
            safetyNotes: step.safetyNotes || "",
            acceptanceCriteria: step.acceptanceCriteria || "",
            preconditions: step.preconditions || "",
            dependencies: step.dependencies || "",
            executionOrder: step.executionOrder || null,
            postConditions: step.postConditions || "",
            passFailCriteria: step.passFailCriteria || "",
            expectedObservation: step.expectedObservation || "",
            engineerRemarks: step.engineerRemarks || "",
        })),
    });
}

export default function ProcedureBuilderPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const isEditing = Boolean(id);

    const [lruId, setLruId] = useState("");
    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [steps, setSteps] = useState([]);
    const [meta, setMeta] = useState(null);

    const [lrus, setLrus] = useState([]);
    const [blocks, setBlocks] = useState([]);

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [submittingForReview, setSubmittingForReview] = useState(false);
    const [error, setError] = useState("");
    const [successMessage, setSuccessMessage] = useState("");
    const [savedDraft, setSavedDraft] = useState(() => createDraftSnapshot());

    // AI-182/AI-183: Version Comparison
    const [showCompare, setShowCompare] = useState(false);
    const [history, setHistory] = useState([]);
    const [fromVersion, setFromVersion] = useState("");
    const [toVersion, setToVersion] = useState("");
    const [comparison, setComparison] = useState(null);
    const [comparing, setComparing] = useState(false);
    const [compareError, setCompareError] = useState("");

    // AI-187 / T7-B3: rollback creates a new revision and preserves history.
    const [rollingBackVersion, setRollingBackVersion] = useState(null);
    const [rollbackError, setRollbackError] = useState("");

    const [coverage, setCoverage] = useState(null);
    const [coverageLoading, setCoverageLoading] = useState(false);
    const [coverageError, setCoverageError] = useState("");
    const [acceptedSuggestions, setAcceptedSuggestions] = useState([]);

    const loadAll = useCallback(async () => {
        try {
            setError("");

            const [lruRes, blockRes] = await Promise.all([
                getLrus(),
                testBlockService.getAllBlocks(),
            ]);

            setLrus(lruRes.data);
            setBlocks(blockRes.data.filter((b) => b.expectedBehaviour));

            if (isEditing) {
                const procRes = await testProcedureService.getProcedureById(id);
                const procedure = procRes.data;

                const loadedLruId = procedure.lruId ? String(procedure.lruId) : "";
                const loadedSteps = (procedure.steps || []).map((s) => ({ ...s }));

                setLruId(loadedLruId);
                setName(procedure.name || "");
                setDescription(procedure.description || "");
                setSteps(loadedSteps);
                setSavedDraft(createDraftSnapshot({
                    lruId: loadedLruId,
                    name: procedure.name || "",
                    description: procedure.description || "",
                    steps: loadedSteps,
                }));

                setMeta({
                    approvalStatus: procedure.approvalStatus,
                    versionNumber: procedure.versionNumber,
                });
                const suggestionsRes = await api.get("/ai/suggestions/accepted", { params: { procedureId: id } });
                setAcceptedSuggestions(suggestionsRes.data.data);
            } else {
                setSavedDraft(createDraftSnapshot());
            }
        } catch (fetchError) {
            console.error("Failed to load Procedure Builder data:", fetchError);
            setError("Unable to load this Test Procedure right now.");
        } finally {
            setLoading(false);
        }
    }, [id, isEditing]);

    useEffect(() => {
        const loadInitialData = async () => {
            await loadAll();
        };

        void loadInitialData();
    }, [loadAll]);

    const currentDraft = createDraftSnapshot({ lruId, name, description, steps });
    const hasUnsavedChanges = currentDraft !== savedDraft;

    useEffect(() => {
        const warnBeforeLeaving = (event) => {
            if (!hasUnsavedChanges) {
                return;
            }
            event.preventDefault();
            event.returnValue = "";
        };

        window.addEventListener("beforeunload", warnBeforeLeaving);
        return () => window.removeEventListener("beforeunload", warnBeforeLeaving);
    }, [hasUnsavedChanges]);

    const handleAddStep = (payload) => {
        setSteps((current) => [
            ...current,
            {
                ...payload,
                id: null,
                _key: nextTempKey(),
            },
        ]);
    };

    const handleUpdateStep = (index, payload) => {
        setSteps((current) =>
            current.map((s, i) =>
                i === index ? { ...s, ...payload } : s
            )
        );
    };

    const applyAcceptedSuggestion = (suggestion) => {
        const stepIndex = steps.findIndex((step) => Number(step.id) === Number(suggestion.testStepId));
        if (stepIndex < 0) {
            setError("This accepted suggestion is not linked to a current step in this procedure.");
            return;
        }
        const attribution = `AI suggestion accepted by ${suggestion.decidedBy} on ${new Date(suggestion.decidedAt).toLocaleString()}: ${suggestion.suggestion}`;
        setSteps((current) => current.map((step, index) => index === stepIndex ? { ...step, engineerRemarks: step.engineerRemarks ? `${step.engineerRemarks}\n${attribution}` : attribution } : step));
        setAcceptedSuggestions((current) => current.filter((item) => item.id !== suggestion.id));
        setSuccessMessage("Accepted suggestion applied locally. Save the procedure to create the attributable revision.");
    };

    const handleDeleteStep = (index) => {
        const isConfirmed = window.confirm(
            "Remove this Test Step? It will be deleted when you save the procedure."
        );

        if (!isConfirmed) {
            return;
        }

        setSteps((current) =>
            current.filter((_, i) => i !== index)
        );
    };

    const handleMove = (index, direction) => {
        const targetIndex = index + direction;

        if (targetIndex < 0 || targetIndex >= steps.length) {
            return;
        }

        setSteps((current) => {
            const reordered = [...current];
            const [moved] = reordered.splice(index, 1);
            reordered.splice(targetIndex, 0, moved);
            return reordered;
        });
    };

    const handleSaveProcedure = async () => {
        setError("");
        setSuccessMessage("");

        if (!lruId) {
            setError("Choose an LRU before saving.");
            return;
        }

        if (!name.trim()) {
            setError("Name is required.");
            return;
        }

        if (!steps.length) {
            setError(
                "Add at least one Test Step before saving - the backend requires every procedure to have at least one step."
            );
            return;
        }

        const payload = {
            lruId: Number(lruId),
            name: name.trim(),
            description: description.trim(),

            steps: steps.map((s, index) => ({
                id: s.id || undefined,
                interfaceId: s.interfaceId || null,
                connectorId: s.connectorId || null,
                signalLibraryId: s.signalLibraryId || null,
                protocolTagId: s.protocolTagId || null,
                faultCategoryId: s.faultCategoryId || null,
                expectedBehaviour: s.expectedBehaviour,
                expectedRange: s.expectedRange,
                calibrationRequirement: s.calibrationRequirement,
                safetyNotes: s.safetyNotes,
                acceptanceCriteria: s.acceptanceCriteria,
                preconditions: s.preconditions,
                dependencies: s.dependencies,
                executionOrder: index + 1,
                postConditions: s.postConditions,
                passFailCriteria: s.passFailCriteria,
                expectedObservation: s.expectedObservation,
                engineerRemarks: s.engineerRemarks,
            })),
        };

        setSaving(true);

        try {
            if (isEditing) {
                const response =
                    await testProcedureService.updateProcedure(id, payload);

                setMeta({
                    approvalStatus: response.data.approvalStatus,
                    versionNumber: response.data.versionNumber,
                });

                const savedSteps = (response.data.steps || []).map((s) => ({ ...s }));
                setSteps(savedSteps);
                setSavedDraft(createDraftSnapshot({
                    lruId,
                    name: payload.name,
                    description: payload.description,
                    steps: savedSteps,
                }));

                setSuccessMessage(
                    `Saved - now at version ${response.data.versionNumber}.`
                );
            } else {
                const response =
                    await testProcedureService.createProcedure(payload);

                navigate(
                    `/procedures/${response.data.id}/builder`,
                    { replace: true }
                );
            }
        } catch (saveError) {
            console.error(
                "Failed to save Test Procedure:",
                saveError
            );

            const message =
                saveError?.response?.data?.message ||
                "Could not save this Test Procedure. Please review the form and try again.";

            setError(message);
        } finally {
            setSaving(false);
        }
    };

    const handleDiscardChanges = async () => {
        if (!hasUnsavedChanges || window.confirm("Discard unsaved procedure changes? This cannot be undone.")) {
            setError("");
            setSuccessMessage("");
            if (isEditing) {
                setLoading(true);
                await loadAll();
                return;
            }
            setLruId("");
            setName("");
            setDescription("");
            setSteps([]);
            setSavedDraft(createDraftSnapshot());
        }
    };

    const handleToggleCompare = async () => {
        const opening = !showCompare;

        setShowCompare(opening);

        if (opening && history.length === 0) {
            try {
                const historyRes =
                    await versionComparisonService.getHistory(id);

                setHistory(historyRes.data);

                if (historyRes.data.length >= 2) {
                    setFromVersion(
                        String(historyRes.data[0].versionNumber)
                    );

                    setToVersion(
                        String(
                            historyRes.data[
                                historyRes.data.length - 1
                            ].versionNumber
                        )
                    );
                }
            } catch (historyError) {
                console.error(
                    "Failed to load revision history:",
                    historyError
                );

                setCompareError(
                    "Unable to load revision history for this Test Procedure."
                );
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
            const compareRes =
                await versionComparisonService.compareVersions(
                    id,
                    fromVersion,
                    toVersion
                );

            setComparison(compareRes.data);
        } catch (compareErrorResponse) {
            console.error(
                "Failed to compare Test Procedure revisions:",
                compareErrorResponse
            );

            setCompareError(
                compareErrorResponse?.response?.data?.message ||
                    "Unable to compare these revisions. Please check the version numbers and try again."
            );
        } finally {
            setComparing(false);
        }
    };

    const handleRollback = async (versionNumber) => {
        const confirmed = window.confirm(
            `Roll back to Revision ${versionNumber}? This creates a new revision and leaves existing revisions unchanged.`
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
        setSuccessMessage("");
        setSubmittingForReview(true);

        try {
            const response = await testProcedureService.submitForReview(id);
            setMeta({
                approvalStatus: response.data.approvalStatus,
                versionNumber: response.data.versionNumber,
            });
            const submittedSteps = (response.data.steps || []).map((s) => ({ ...s }));
            setSteps(submittedSteps);
            setSavedDraft(createDraftSnapshot({
                lruId,
                name,
                description,
                steps: submittedSteps,
            }));
            setSuccessMessage(`Submitted for review - now at version ${response.data.versionNumber}.`);
        } catch (submitError) {
            console.error("Failed to submit Test Procedure for review:", submitError);
            const message = submitError?.response?.data?.message
                || "Could not submit this Test Procedure for review. Save the procedure and resolve any validation errors, then try again.";
            setError(message);
        } finally {
            setSubmittingForReview(false);
        }
    };

    const handleEvaluateCoverage = async () => {
        if (!isEditing) return;
        setCoverageLoading(true);
        setCoverageError("");
        try {
            const response = await testProcedureService.getCoverage(id);
            setCoverage(response.data);
        } catch (coverageRequestError) {
            setCoverage(null);
            setCoverageError(
                coverageRequestError?.response?.data?.message ||
                "Unable to evaluate coverage for this procedure. Save the revision and try again."
            );
        } finally {
            setCoverageLoading(false);
        }
    };


    if (loading) {
        return (
            <ManagementLayout
                title="Procedure Builder"
                description="Loading..."
            >
                <p className="helper-text">
                    Loading Procedure Builder...
                </p>
            </ManagementLayout>
        );
    }

    const lruName = lrus.find(
        (l) => String(l.id) === String(lruId)
    )?.name;

    return (
        <ManagementLayout
            title={
                isEditing
                    ? `Procedure Builder - ${name || "..."}`
                    : "Create Test Procedure"
            }
            description={
                isEditing && meta
                    ? `LRU: ${lruName || "-"}  |  Status: ${meta.approvalStatus}  |  Version: ${meta.versionNumber}`
                    : "A Test Procedure belongs to a single LRU; its Test Steps can only reference that LRU's own Interfaces, Connectors, Signals, Protocol Tags and Fault Templates. At least one Test Step is required."
            }
        >
            <section className="procedure-builder__status" aria-label="Procedure draft status">
                <div>
                    <p className="eyebrow">Authoring status</p>
                    <strong>{hasUnsavedChanges ? "Unsaved changes" : "All changes saved"}</strong>
                    <p className="helper-text">{hasUnsavedChanges ? "Save before submitting for review or leaving this page." : "Your current procedure revision is safely stored."}</p>
                </div>
                {isEditing && meta ? <span className={`status-pill status-pill--${String(meta.approvalStatus || "draft").toLowerCase().replaceAll("_", "-")}`}>{meta.approvalStatus} · v{meta.versionNumber}</span> : <span className="status-pill">DRAFT</span>}
            </section>
            {isEditing && acceptedSuggestions.length ? (
                <section className="form-card">
                    <h2>Accepted AI suggestions</h2>
                    <p className="helper-text">Apply a suggestion to the unsaved draft, then save to create the formal attributable revision.</p>
                    {acceptedSuggestions.map((suggestion) => (
                        <article key={suggestion.id} className="page-card" style={{ margin: "10px 0 0" }}>
                            <p>{suggestion.suggestion}</p>
                            <p className="helper-text">Step {suggestion.testStepId ?? "not specified"} · accepted by {suggestion.decidedBy}</p>
                            <button onClick={() => applyAcceptedSuggestion(suggestion)} disabled={!suggestion.testStepId}>Apply to step remarks</button>
                        </article>
                    ))}
                </section>
            ) : null}
            {error ? (
                <p style={{ color: "var(--danger)" }}>
                    {error}
                </p>
            ) : null}

            {successMessage ? (
                <p className="helper-text">
                    {successMessage}
                </p>
            ) : null}

            <section className="form-card">
                <p className="helper-text procedure-builder__required-note">Fields marked <strong>*</strong> are required. Save each Test Step after completing its required evidence fields, then save the procedure revision.</p>
                <div className="form-grid form-grid--two">
                    <div className="form-field">
                        <label>LRU *</label>

                        <select
                            value={lruId}
                            onChange={(e) =>
                                setLruId(e.target.value)
                            }
                        >
                            <option value="">
                                Select an LRU
                            </option>

                            {lrus.map((l) => (
                                <option
                                    key={l.id}
                                    value={l.id}
                                >
                                    {l.name}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="form-field">
                        <label>Name *</label>

                        <input
                            type="text"
                            value={name}
                            onChange={(e) =>
                                setName(e.target.value)
                            }
                        />
                    </div>

                    <div
                        className="form-field"
                        style={{
                            gridColumn: "1 / -1",
                        }}
                    >
                        <label>Description</label>

                        <textarea
                            value={description}
                            onChange={(e) =>
                                setDescription(e.target.value)
                            }
                            rows={2}
                        />
                    </div>
                </div>
            </section>

            <section
                className="stack"
                style={{ marginTop: "16px" }}
            >
                {!steps.length ? (
                    <div className="empty-state">
                        No Test Steps yet. Add the first one below.
                    </div>
                ) : (
                    steps.map((step, index) => (
                        <TestStepCard
                            key={step.id || step._key}
                            step={step}
                            lruId={lruId}
                            onSave={(payload) =>
                                handleUpdateStep(index, payload)
                            }
                            onDelete={() =>
                                handleDeleteStep(index)
                            }
                            onMoveUp={() =>
                                handleMove(index, -1)
                            }
                            onMoveDown={() =>
                                handleMove(index, 1)
                            }
                            isFirst={index === 0}
                            isLast={
                                index === steps.length - 1
                            }
                        />
                    ))
                )}

                <TestStepCard
                    step={null}
                    lruId={lruId}
                    onSave={handleAddStep}
                />
            </section>

            {isEditing ? (
                <InsertTestBlockPanel
                    procedureId={id}
                    lruId={lruId}
                    blocks={blocks}
                    onInserted={loadAll}
                />
            ) : null}

            {isEditing ? (
                <CoverageSummaryCard
                    coverage={coverage}
                    loading={coverageLoading}
                    error={coverageError}
                    onRefresh={handleEvaluateCoverage}
                />
            ) : null}

            <section
                className="stack"
                style={{ marginTop: "28px" }}
            >
                <div className="page-toolbar">
                    <h2
                        className="section-title"
                        style={{ margin: 0 }}
                    >
                        Version Comparison
                    </h2>

                    <button
                        type="button"
                        className="button--secondary"
                        onClick={handleToggleCompare}
                    >
                        {showCompare
                            ? "Hide"
                            : "Compare Revisions"}
                    </button>
                </div>

                {showCompare && (
                    <div className="page-card">
                        {history.length < 2 ? (
                            <p className="helper-text">
                                This Test Procedure needs at
                                least two saved revisions before
                                versions can be compared.
                            </p>
                        ) : (
                            <>
                                <div className="form-grid form-grid--two">
                                    <div className="form-field">
                                        <label>
                                            From Revision
                                        </label>

                                        <select
                                            value={fromVersion}
                                            onChange={(e) =>
                                                setFromVersion(
                                                    e.target.value
                                                )
                                            }
                                        >
                                            {history.map((h) => (
                                                <option
                                                    key={
                                                        h.versionNumber
                                                    }
                                                    value={
                                                        h.versionNumber
                                                    }
                                                >
                                                    Revision{" "}
                                                    {
                                                        h.versionNumber
                                                    }
                                                </option>
                                            ))}
                                        </select>
                                    </div>

                                    <div className="form-field">
                                        <label>
                                            To Revision
                                        </label>

                                        <select
                                            value={toVersion}
                                            onChange={(e) =>
                                                setToVersion(
                                                    e.target.value
                                                )
                                            }
                                        >
                                            {history.map((h) => (
                                                <option
                                                    key={
                                                        h.versionNumber
                                                    }
                                                    value={
                                                        h.versionNumber
                                                    }
                                                >
                                                    Revision{" "}
                                                    {
                                                        h.versionNumber
                                                    }
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                </div>

                                <div className="form-actions">
                                    <button
                                        type="button"
                                        onClick={handleCompare}
                                        disabled={comparing}
                                    >
                                        {comparing
                                            ? "Comparing..."
                                            : "Compare"}
                                    </button>
                                </div>
                            </>
                        )}

                        {compareError ? (
                            <p
                                style={{
                                    color: "var(--danger)",
                                }}
                            >
                                {compareError}
                            </p>
                        ) : null}

                        {history.length ? (
                            <div className="stack" style={{ marginTop: "18px" }}>
                                <p className="helper-text">
                                    Rollback creates a new revision from the selected revision; no existing revision is modified.
                                </p>
                                {history.map((revision) => (
                                    <div className="page-toolbar" key={revision.versionNumber}>
                                        <span>Revision {revision.versionNumber}</span>
                                        <button
                                            type="button"
                                            className="button--secondary"
                                            onClick={() => handleRollback(revision.versionNumber)}
                                            disabled={rollingBackVersion !== null}
                                        >
                                            {rollingBackVersion === revision.versionNumber ? "Rolling back..." : "Rollback"}
                                        </button>
                                    </div>
                                ))}
                                {rollbackError ? <p style={{ color: "var(--danger)" }}>{rollbackError}</p> : null}
                            </div>
                        ) : null}

                        {comparison && (
                            <div
                                className="stack"
                                style={{
                                    marginTop: "18px",
                                }}
                            >
                                <p className="helper-text">
                                    Revision{" "}
                                    {comparison.fromVersion}
                                    {" → "}
                                    {comparison.toVersion}
                                    {" · "}
                                    {
                                        comparison.addedStepCount
                                    }{" "}
                                    added,{" "}
                                    {
                                        comparison.removedStepCount
                                    }{" "}
                                    removed,{" "}
                                    {
                                        comparison.changedStepCount
                                    }{" "}
                                    changed,{" "}
                                    {
                                        comparison.unchangedStepCount
                                    }{" "}
                                    unchanged
                                </p>

                                {comparison.steps.map(
                                    (step, index) => (
                                        <StepComparisonCard
                                            key={
                                                step.toStep?.id ??
                                                step.fromStep?.id ??
                                                index
                                            }
                                            step={step}
                                        />
                                    )
                                )}
                            </div>
                        )}
                    </div>
                )}
            </section>

            <div className="form-actions" style={{ marginTop: "16px" }}>
                <button onClick={handleSaveProcedure} disabled={saving || submittingForReview || !hasUnsavedChanges}>
                    {saving ? "Saving..." : hasUnsavedChanges ? "Save Procedure" : "Saved"}
                </button>
                <button type="button" className="button--secondary" onClick={() => void handleDiscardChanges()} disabled={saving || submittingForReview || !hasUnsavedChanges}>
                    Discard changes
                </button>
                {isEditing && meta?.approvalStatus === "DRAFT" ? (
                    <button onClick={handleSubmitForReview} disabled={saving || submittingForReview}>
                        {submittingForReview ? "Submitting..." : "Submit for Review"}
                    </button>
                ) : null}
            </div>
        </ManagementLayout>
    );
}

function InsertTestBlockPanel({
    procedureId,
    lruId,
    blocks,
    onInserted,
}) {
    const [blockId, setBlockId] = useState("");
    const [interfaceId, setInterfaceId] = useState("");
    const [connectorId, setConnectorId] = useState("");
    const [signalLibraryId, setSignalLibraryId] =
        useState("");
    const [protocolTagId, setProtocolTagId] = useState("");
    const [faultCategoryId, setFaultCategoryId] =
        useState("");

    const [interfaces, setInterfaces] = useState([]);
    const [connectors, setConnectors] = useState([]);
    const [signalLibraries, setSignalLibraries] =
        useState([]);
    const [protocolTags, setProtocolTags] = useState([]);
    const [faultTemplates, setFaultTemplates] =
        useState([]);

    const [inserting, setInserting] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!lruId) {
            return;
        }

        const loadOptions = async () => {
            try {
                const [
                    ifaceRes,
                    connRes,
                    sigRes,
                    protoRes,
                    faultRes,
                ] = await Promise.all([
                    interfaceService.getInterfacesByLruId(
                        lruId
                    ),
                    connectorService.getConnectorsByLruId(
                        lruId
                    ),
                    signalLibraryService.getSignalLibrariesByLruId(
                        lruId
                    ),
                    protocolTagService.getProtocolTagsByLruId(
                        lruId
                    ),
                    faultTemplateService.getFaultTemplatesByLruId(
                        lruId
                    ),
                ]);

                setInterfaces(ifaceRes.data);
                setConnectors(connRes.data);
                setSignalLibraries(sigRes.data);
                setProtocolTags(protoRes.data);
                setFaultTemplates(faultRes.data);
            } catch (fetchError) {
                console.error(
                    "Failed to load reference options for Test Block insert:",
                    fetchError
                );
            }
        };

        void loadOptions();
    }, [lruId]);

    const handleInsert = async () => {
        setError("");

        if (
            !blockId ||
            !interfaceId ||
            !connectorId ||
            !signalLibraryId ||
            !protocolTagId ||
            !faultCategoryId
        ) {
            setError(
                "Choose a Test Block and all five bindings before inserting."
            );
            return;
        }

        setInserting(true);

        try {
            await testBlockService.insertIntoProcedure(
                blockId,
                {
                    testProcedureId: Number(procedureId),
                    interfaceId: Number(interfaceId),
                    connectorId: Number(connectorId),
                    signalLibraryId: Number(
                        signalLibraryId
                    ),
                    protocolTagId: Number(protocolTagId),
                    faultCategoryId: Number(
                        faultCategoryId
                    ),
                }
            );

            setBlockId("");
            setInterfaceId("");
            setConnectorId("");
            setSignalLibraryId("");
            setProtocolTagId("");
            setFaultCategoryId("");

            await onInserted();
        } catch (insertError) {
            console.error(
                "Failed to insert Test Block into procedure:",
                insertError
            );

            const message =
                insertError?.response?.data?.message ||
                "Could not insert this Test Block.";

            setError(message);
        } finally {
            setInserting(false);
        }
    };

    if (!blocks.length) {
        return null;
    }

    return (
        <section
            className="form-card"
            style={{ marginTop: "16px" }}
        >
            <p className="helper-text">
                Insert a reusable Test Block from the shared
                library as a new step in this procedure.
            </p>

            <div className="form-grid form-grid--two">
                <div className="form-field">
                    <label>Test Block *</label>

                    <select
                        value={blockId}
                        onChange={(e) =>
                            setBlockId(e.target.value)
                        }
                    >
                        <option value="">
                            Select a Test Block
                        </option>

                        {blocks.map((b) => (
                            <option
                                key={b.id}
                                value={b.id}
                            >
                                {b.name} (used{" "}
                                {b.usageCount}x)
                            </option>
                        ))}
                    </select>
                </div>

                <div className="form-field">
                    <label>Interface *</label>

                    <select
                        value={interfaceId}
                        onChange={(e) =>
                            setInterfaceId(e.target.value)
                        }
                    >
                        <option value="">Select</option>

                        {interfaces.map((i) => (
                            <option
                                key={i.id}
                                value={i.id}
                            >
                                {i.name}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="form-field">
                    <label>Connector *</label>

                    <select
                        value={connectorId}
                        onChange={(e) =>
                            setConnectorId(e.target.value)
                        }
                    >
                        <option value="">Select</option>

                        {connectors.map((c) => (
                            <option
                                key={c.id}
                                value={c.id}
                            >
                                {c.name}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="form-field">
                    <label>Signal Library *</label>

                    <select
                        value={signalLibraryId}
                        onChange={(e) =>
                            setSignalLibraryId(
                                e.target.value
                            )
                        }
                    >
                        <option value="">Select</option>

                        {signalLibraries.map((s) => (
                            <option
                                key={s.id}
                                value={s.id}
                            >
                                {s.name}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="form-field">
                    <label>Protocol Tag *</label>

                    <select
                        value={protocolTagId}
                        onChange={(e) =>
                            setProtocolTagId(
                                e.target.value
                            )
                        }
                    >
                        <option value="">Select</option>

                        {protocolTags.map((p) => (
                            <option
                                key={p.id}
                                value={p.id}
                            >
                                {p.name}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="form-field">
                    <label>Fault Category *</label>

                    <select
                        value={faultCategoryId}
                        onChange={(e) =>
                            setFaultCategoryId(
                                e.target.value
                            )
                        }
                    >
                        <option value="">Select</option>

                        {faultTemplates.map((f) => (
                            <option
                                key={f.id}
                                value={f.id}
                            >
                                {f.name}
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            {error ? (
                <p style={{ color: "var(--danger)" }}>
                    {error}
                </p>
            ) : null}

            <div className="form-actions">
                <button
                    onClick={handleInsert}
                    disabled={inserting}
                >
                    {inserting
                        ? "Inserting..."
                        : "Insert Test Block"}
                </button>
            </div>
        </section>
    );
}
