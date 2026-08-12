import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import TestStepCard from "../components/TestStepCard";
import { getLrus } from "../services/lruService";
import { interfaceService } from "../services/interfaceService";
import { connectorService } from "../services/connectorService";
import { signalLibraryService } from "../services/signalLibraryService";
import { protocolTagService } from "../services/protocolTagService";
import { faultTemplateService } from "../services/faultTemplateService";
import { testProcedureService } from "../services/testProcedureService";
import { reviewerCommentService } from "../services/reviewerCommentService";

let clientKeySeq = 0;
const nextClientKey = () => `new-${++clientKeySeq}`;

const EMPTY_STEP = () => ({
    _clientKey: nextClientKey(),
    id: null,
    interfaceId: "",
    connectorId: "",
    signalLibraryId: "",
    protocolTagId: "",
    faultCategoryId: "",
    expectedBehaviour: "",
    expectedRange: "",
    acceptanceCriteria: "",
    passFailCriteria: "",
});

export default function ProcedureBuilderPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const isNew = !id || id === "new";

    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [lruId, setLruId] = useState("");
    const [steps, setSteps] = useState([]);
    const [approvalStatus, setApprovalStatus] = useState(null);
    const [versionNumber, setVersionNumber] = useState(null);

    const [lrus, setLrus] = useState([]);
    const [interfaces, setInterfaces] = useState([]);
    const [connectors, setConnectors] = useState([]);
    const [signalLibraries, setSignalLibraries] = useState([]);
    const [protocolTags, setProtocolTags] = useState([]);
    const [faultTemplates, setFaultTemplates] = useState([]);

    const [history, setHistory] = useState([]);
    const [comments, setComments] = useState([]);
    const [newComment, setNewComment] = useState("");
    const [commentStepId, setCommentStepId] = useState("");

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");
    const [fieldErrors, setFieldErrors] = useState({});
    const [savedMessage, setSavedMessage] = useState("");

    // Load reference data + (if editing) the existing procedure.
    useEffect(() => {
        const load = async () => {
            try {
                setError("");
                const [lruRes, ifaceRes] = await Promise.all([
                    getLrus(),
                    interfaceService.getAllInterfaces(),
                ]);
                setLrus(lruRes.data);
                setInterfaces(ifaceRes.data);

                if (!isNew) {
                    const procRes = await testProcedureService.getProcedureById(id);
                    const p = procRes.data;
                    setName(p.name);
                    setDescription(p.description || "");
                    setLruId(String(p.lruId));
                    setApprovalStatus(p.approvalStatus);
                    setVersionNumber(p.versionNumber);
                    setSteps(
                        p.steps.map((s) => ({
                            _clientKey: nextClientKey(),
                            id: s.id,
                            interfaceId: s.interfaceId ? String(s.interfaceId) : "",
                            connectorId: s.connectorId ? String(s.connectorId) : "",
                            signalLibraryId: s.signalLibraryId ? String(s.signalLibraryId) : "",
                            protocolTagId: s.protocolTagId ? String(s.protocolTagId) : "",
                            faultCategoryId: s.faultCategoryId ? String(s.faultCategoryId) : "",
                            expectedBehaviour: s.expectedBehaviour || "",
                            expectedRange: s.expectedRange || "",
                            acceptanceCriteria: s.acceptanceCriteria || "",
                            passFailCriteria: s.passFailCriteria || "",
                            approvalStatus: s.approvalStatus,
                        }))
                    );

                    const historyRes = await testProcedureService.getHistory(id);
                    setHistory(historyRes.data);

                    if (historyRes.data.length > 0) {
                        const latestRevision = historyRes.data[historyRes.data.length - 1];
                        const commentsRes = await reviewerCommentService.getByRevision(latestRevision.id);
                        setComments(commentsRes.data);
                    }
                } else {
                    setSteps([EMPTY_STEP()]);
                }
            } catch (fetchError) {
                console.error("Failed to load procedure builder data:", fetchError);
                setError("Unable to load the Procedure Builder right now.");
            } finally {
                setLoading(false);
            }
        };

        void load();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [id]);

    // Load LRU-scoped typed reference options once an LRU is selected (Part M).
    useEffect(() => {
        if (!lruId) {
            return;
        }

        const loadScoped = async () => {
            try {
                const [connRes, sigRes, protoRes, faultRes] = await Promise.all([
                    connectorService.getAllConnectors(),
                    signalLibraryService.getSignalLibrariesByLruId(lruId),
                    protocolTagService.getProtocolTagsByLruId(lruId),
                    faultTemplateService.getFaultTemplatesByLruId(lruId),
                ]);
                setConnectors(connRes.data.filter((c) => String(c.lru?.id) === String(lruId)));
                setSignalLibraries(sigRes.data);
                setProtocolTags(protoRes.data);
                setFaultTemplates(faultRes.data);
            } catch (scopedError) {
                console.error("Failed to load LRU-scoped options:", scopedError);
            }
        };

        void loadScoped();
    }, [lruId]);

    const scopedInterfaces = useMemo(
        () => (lruId ? interfaces.filter((i) => String(i.lru?.id) === String(lruId)) : []),
        [interfaces, lruId]
    );

    const handleAddStep = () => {
        setSteps((current) => [...current, EMPTY_STEP()]);
    };

    const handleStepChange = (index, updatedStep) => {
        setSteps((current) => current.map((s, i) => (i === index ? updatedStep : s)));
    };

    const handleRemoveStep = (index) => {
        setSteps((current) => current.filter((_, i) => i !== index));
    };

    const handleMove = (index, direction) => {
        setSteps((current) => {
            const target = index + direction;
            if (target < 0 || target >= current.length) return current;
            const copy = [...current];
            [copy[index], copy[target]] = [copy[target], copy[index]];
            return copy;
        });
    };

    const validate = () => {
        const errs = {};
        if (!lruId) errs.lruId = "LRU is required.";
        if (!name.trim()) errs.name = "Name is required.";
        if (steps.length === 0) errs.steps = "At least one Test Step is required.";

        const stepErrors = steps.map((s) => {
            const stepErrs = {};
            if (!s.interfaceId) stepErrs.interfaceId = "Interface is required.";
            if (!s.expectedBehaviour.trim()) stepErrs.expectedBehaviour = "Required.";
            return stepErrs;
        });

        return { errs, stepErrors };
    };

    const handleSave = async () => {
        setError("");
        setSavedMessage("");

        const { errs, stepErrors } = validate();
        setFieldErrors({ top: errs, steps: stepErrors });
        if (Object.keys(errs).length > 0 || stepErrors.some((e) => Object.keys(e).length > 0)) {
            setError("Please fix the highlighted fields before saving.");
            return;
        }

        const payload = {
            lruId: Number(lruId),
            name: name.trim(),
            description: description.trim(),
            steps: steps.map((s, idx) => ({
                id: s.id || null,
                interfaceId: Number(s.interfaceId),
                connectorId: Number(s.connectorId),
                signalLibraryId: Number(s.signalLibraryId),
                protocolTagId: Number(s.protocolTagId),
                faultCategoryId: Number(s.faultCategoryId),
                expectedBehaviour: s.expectedBehaviour.trim(),
                expectedRange: s.expectedRange?.trim() || null,
                acceptanceCriteria: s.acceptanceCriteria.trim(),
                passFailCriteria: s.passFailCriteria.trim(),
                executionOrder: idx + 1,
            })),
        };

        setSaving(true);
        try {
            const response = isNew
                ? await testProcedureService.createProcedure(payload)
                : await testProcedureService.updateProcedure(id, payload);

            const saved = response.data;
            setVersionNumber(saved.versionNumber);
            setApprovalStatus(saved.approvalStatus);
            setSavedMessage(`Saved as revision ${saved.versionNumber}.`);

            if (isNew) {
                navigate(`/procedures/${saved.id}`, { replace: true });
            } else {
                const historyRes = await testProcedureService.getHistory(id);
                setHistory(historyRes.data);
            }
        } catch (saveError) {
            console.error("Failed to save procedure:", saveError);
            setError(
                saveError.response?.data?.message ||
                    "Could not save the procedure. Please review the form and try again."
            );
        } finally {
            setSaving(false);
        }
    };

    const handleAddComment = async () => {
        if (!newComment.trim() || history.length === 0) return;

        const latestRevision = history[history.length - 1];

        try {
            await reviewerCommentService.createComment({
                procedureHistoryId: latestRevision.id,
                testStepId: commentStepId || null,
                commentText: newComment.trim(),
            });
            const commentsRes = await reviewerCommentService.getByRevision(latestRevision.id);
            setComments(commentsRes.data);
            setNewComment("");
            setCommentStepId("");
        } catch (commentError) {
            console.error("Failed to add comment:", commentError);
            setError("Could not add the comment.");
        }
    };

    if (loading) {
        return (
            <ManagementLayout title="Procedure Builder" description="Loading…">
                <p className="helper-text">Loading Procedure Builder…</p>
            </ManagementLayout>
        );
    }

    return (
        <ManagementLayout
            title={isNew ? "New Test Procedure" : `Edit: ${name}`}
            description="Assemble a Test Procedure from typed Test Steps. Every saved change creates a new immutable revision."
        >
            {error ? <p style={{ color: "var(--danger)" }}>{error}</p> : null}
            {savedMessage ? <p style={{ color: "var(--success, green)" }}>{savedMessage}</p> : null}

            <section className="page-card stack">
                <div className="form-grid form-grid--two">
                    <div className="form-field">
                        <label>Procedure Name</label>
                        <input
                            type="text"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            required
                        />
                        {fieldErrors.top?.name ? (
                            <p style={{ color: "var(--danger)" }}>{fieldErrors.top.name}</p>
                        ) : null}
                    </div>

                    <div className="form-field">
                        <label>LRU</label>
                        <select value={lruId} onChange={(e) => setLruId(e.target.value)} required>
                            <option value="">Select LRU</option>
                            {lrus.map((l) => (
                                <option key={l.id} value={l.id}>
                                    {l.name}
                                </option>
                            ))}
                        </select>
                        {fieldErrors.top?.lruId ? (
                            <p style={{ color: "var(--danger)" }}>{fieldErrors.top.lruId}</p>
                        ) : null}
                    </div>
                </div>

                <div className="form-field">
                    <label>Description</label>
                    <textarea
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        rows={3}
                    />
                </div>

                {!isNew ? (
                    <p className="helper-text">
                        Approval status: {approvalStatus} · Current version: {versionNumber}
                    </p>
                ) : null}
            </section>

            <section className="stack" style={{ marginTop: "16px" }}>
                <div className="page-toolbar">
                    <h2>Test Steps</h2>
                    <button type="button" onClick={handleAddStep} disabled={!lruId}>
                        Add Test Step
                    </button>
                </div>

                {!lruId ? (
                    <p className="helper-text">Select an LRU to enable typed reference selection.</p>
                ) : null}

                {fieldErrors.top?.steps ? (
                    <p style={{ color: "var(--danger)" }}>{fieldErrors.top.steps}</p>
                ) : null}

                {steps.map((step, index) => (
                    <TestStepCard
                        key={step._clientKey}
                        step={step}
                        index={index}
                        total={steps.length}
                        interfaces={scopedInterfaces}
                        connectors={connectors}
                        signalLibraries={signalLibraries}
                        protocolTags={protocolTags}
                        faultTemplates={faultTemplates}
                        onChange={(updated) => handleStepChange(index, updated)}
                        onRemove={() => handleRemoveStep(index)}
                        onMoveUp={() => handleMove(index, -1)}
                        onMoveDown={() => handleMove(index, 1)}
                        errors={fieldErrors.steps?.[index] || {}}
                    />
                ))}
            </section>

            <div className="form-actions" style={{ marginTop: "16px" }}>
                <button type="button" onClick={handleSave} disabled={saving}>
                    {saving ? "Saving…" : "Save Procedure"}
                </button>
            </div>

            {!isNew ? (
                <section className="page-card stack" style={{ marginTop: "24px" }}>
                    <h2>Revision History</h2>
                    {history.length === 0 ? (
                        <p className="helper-text">No revisions yet.</p>
                    ) : (
                        <table>
                            <thead>
                                <tr>
                                    <th>Version</th>
                                    <th>Name</th>
                                    <th>Approval Status</th>
                                    <th>Created By</th>
                                    <th>Created At</th>
                                </tr>
                            </thead>
                            <tbody>
                                {history.map((h) => (
                                    <tr key={h.id}>
                                        <td>{h.versionNumber}</td>
                                        <td>{h.name}</td>
                                        <td>{h.approvalStatus}</td>
                                        <td>{h.createdBy}</td>
                                        <td>{h.createdAt}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </section>
            ) : null}

            {!isNew && history.length > 0 ? (
                <section className="page-card stack" style={{ marginTop: "24px" }}>
                    <h2>Reviewer Comments (latest revision)</h2>

                    {comments.length === 0 ? (
                        <p className="helper-text">No comments yet on the latest revision.</p>
                    ) : (
                        <ul>
                            {comments.map((c) => (
                                <li key={c.id}>
                                    <strong>{c.author}</strong>
                                    {c.testStepId ? ` (Step ${c.testStepId})` : ""}: {c.commentText}
                                </li>
                            ))}
                        </ul>
                    )}

                    <div className="form-grid form-grid--two">
                        <div className="form-field">
                            <label>Anchor to Test Step (optional)</label>
                            <select value={commentStepId} onChange={(e) => setCommentStepId(e.target.value)}>
                                <option value="">Whole revision</option>
                                {steps
                                    .filter((s) => s.id)
                                    .map((s) => (
                                        <option key={s.id} value={s.id}>
                                            Step {s.id}
                                        </option>
                                    ))}
                            </select>
                        </div>

                        <div className="form-field">
                            <label>Comment</label>
                            <textarea
                                value={newComment}
                                onChange={(e) => setNewComment(e.target.value)}
                                rows={2}
                            />
                        </div>
                    </div>

                    <button type="button" onClick={handleAddComment}>
                        Add Comment
                    </button>
                </section>
            ) : null}
        </ManagementLayout>
    );
}
