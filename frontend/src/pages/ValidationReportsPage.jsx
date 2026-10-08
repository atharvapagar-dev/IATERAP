import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import ManagementLayout from "../components/ManagementLayout";
import { validationService } from "../services/validationService";
import { testProcedureService } from "../services/testProcedureService";
import { reviewerCommentService } from "../services/reviewerCommentService";
import "../assets/styles/ValidationReports.css";

const resultLabel = {
    MATCH: "Matched",
    MISMATCH: "Mismatched",
    OUTLIER: "Outlier",
    MISSING: "Missing",
};

export default function ValidationReportsPage() {
    const [procedures, setProcedures] = useState([]);
    const [revisions, setRevisions] = useState([]);
    const [procedureId, setProcedureId] = useState("");
    const [historyId, setHistoryId] = useState("");
    const [runs, setRuns] = useState([]);
    const [selectedRun, setSelectedRun] = useState(null);
    const [reportData, setReportData] = useState(null);
    const [readings, setReadings] = useState([]);
    const [file, setFile] = useState(null);
    const [format, setFormat] = useState("CSV");
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const [successMessage, setSuccessMessage] = useState("");
    const [injectionBusy, setInjectionBusy] = useState(false);
    const [addressed, setAddressed] = useState(() => new Set());

    // Synthetic injection options (T13-B1, T13-B3)
    const [faultCategory, setFaultCategory] = useState("OUT_OF_RANGE");
    const [targetStepId, setTargetStepId] = useState("");

    useEffect(() => {
        testProcedureService
            .getAllProcedures()
            .then((r) => setProcedures(r.data || []))
            .catch(() => setError("Unable to load procedures."));
    }, []);

    const selectedRevision = useMemo(() => {
        return revisions.find((r) => String(r.id) === String(historyId)) || null;
    }, [revisions, historyId]);

    const revisionSteps = useMemo(() => {
        if (!selectedRevision?.stepsSnapshot) return [];
        try {
            const parsed = JSON.parse(selectedRevision.stepsSnapshot);
            return Array.isArray(parsed) ? parsed : [];
        } catch {
            return [];
        }
    }, [selectedRevision]);

    const isRevisionApproved = selectedRevision?.approvalStatus === "APPROVED";

    const selectProcedure = async (value) => {
        setProcedureId(value);
        setHistoryId("");
        setRevisions([]);
        setRuns([]);
        setSelectedRun(null);
        setReportData(null);
        setReadings([]);
        setTargetStepId("");
        setError("");
        setSuccessMessage("");
        if (!value) return;
        try {
            const resp = await testProcedureService.getHistory(value);
            setRevisions(resp.data || []);
        } catch {
            setError("Unable to load procedure revisions.");
        }
    };

    const selectRevision = async (value) => {
        setHistoryId(value);
        setSelectedRun(null);
        setReportData(null);
        setReadings([]);
        setTargetStepId("");
        setAddressed(new Set());
        setError("");
        setSuccessMessage("");
        if (value) await loadRuns(value);
    };

    const loadRuns = async (revision = historyId) => {
        if (!revision) return;
        setError("");
        try {
            const response = await validationService.listRuns(revision);
            setRuns(response.data || []);
            if (response.data?.length) {
                await selectRun(response.data[0]);
            }
        } catch (err) {
            setError(err.response?.data?.message || "Unable to load validation reports.");
        }
    };

    const selectRun = async (run) => {
        setSelectedRun(run);
        setError("");
        try {
            const reportResp = await validationService.getReport(run.id);
            setReportData(reportResp.data);
            setReadings(reportResp.data.readings || []);
        } catch {
            // Fallback to raw readings endpoint
            try {
                const readingsResp = await validationService.getReadings(run.id);
                setReadings(readingsResp.data || []);
                setReportData(null);
            } catch {
                setError("Unable to load report readings.");
            }
        }
    };

    const upload = async (event) => {
        event.preventDefault();
        if (!historyId || !file) {
            setError("Select a procedure revision and choose a CSV or JSON fixture.");
            return;
        }
        setBusy(true);
        setError("");
        setSuccessMessage("");
        try {
            const response = await validationService.upload(historyId, format, file);
            setRuns((current) => [response.data, ...current]);
            await selectRun(response.data);
            setFile(null);
            event.target.reset();
            setSuccessMessage(`Hardware telemetry fixture uploaded successfully as Run #${response.data.id}.`);
        } catch (err) {
            setError(err.response?.data?.message || "Fixture upload failed. Check the revision and file format.");
        } finally {
            setBusy(false);
        }
    };

    const triggerSynthetic = async (scenario) => {
        if (!historyId) {
            setError("Select an approved procedure revision before running synthetic injection.");
            return;
        }
        if (!isRevisionApproved) {
            setError("Synthetic fault injection requires an APPROVED procedure revision.");
            return;
        }
        setInjectionBusy(true);
        setError("");
        setSuccessMessage("");
        try {
            const options = {
                faultCategory: scenario === "FAULT_INJECTED" ? faultCategory : undefined,
                targetLruId: selectedRevision?.lruId || undefined,
                targetStepId: targetStepId ? Number(targetStepId) : undefined,
            };
            const response = await validationService.runSynthetic(historyId, scenario, options);
            setRuns((current) => [response.data, ...current]);
            await selectRun(response.data);
            setSuccessMessage(
                scenario === "FAULT_INJECTED"
                    ? `Synthetic fault injection (${faultCategory}) completed successfully as Run #${response.data.id}.`
                    : `Nominal synthetic simulation completed successfully as Run #${response.data.id}.`
            );
        } catch (err) {
            if (err.response?.status === 403) {
                setError("Authorization Error: You do not have permission to trigger synthetic fault injection. Permitted roles: ADMIN, TEST_ENGINEER, SYSTEM_ENGINEER, RELIABILITY_ENGINEER.");
            } else {
                setError(err.response?.data?.message || "Synthetic injection failed. Verify procedure snapshot and status.");
            }
        } finally {
            setInjectionBusy(false);
        }
    };

    const issues = useMemo(() => readings.filter((reading) => reading.result !== "MATCH"), [readings]);

    const markAddressed = async (reading) => {
        try {
            await reviewerCommentService.createComment({
                procedureHistoryId: Number(historyId),
                testStepId: reading.testStepId,
                commentText: `Validation remediation recorded for ${resultLabel[reading.result] || reading.result} reading from ${selectedRun?.sourceFilename}. ${reading.errorMessage || "Review evidence and update the procedure if required."}`,
            });
            setAddressed((current) => new Set([...current, reading.id]));
        } catch {
            setError("Unable to persist the remediation note.");
        }
    };

    const isSyntheticEvidence = reportData?.synthetic || selectedRun?.sourceFilename?.toLowerCase().startsWith("synthetic-");

    const passRate = useMemo(() => {
        if (reportData?.passRatePercentage != null) return reportData.passRatePercentage;
        if (!selectedRun || selectedRun.totalReadings === 0) return 0;
        return ((selectedRun.matchedCount / selectedRun.totalReadings) * 100).toFixed(2);
    }, [reportData, selectedRun]);

    return (
        <ManagementLayout
            title="Validation reports"
            description="Compare external and synthetic evidence against an immutable procedure revision to verify reliability and close discrepancies."
        >
            <section className="page-card validation-toolbar">
                <div>
                    <p className="eyebrow">Evidence ingestion & synthetic injection</p>
                    <h2 className="section-title">Run an independent validation report</h2>
                </div>

                <form onSubmit={upload} className="validation-upload-form">
                    <label>
                        Procedure
                        <select value={procedureId} onChange={(e) => selectProcedure(e.target.value)}>
                            <option value="">Select procedure</option>
                            {procedures.map((procedure) => (
                                <option key={procedure.id} value={procedure.id}>
                                    {procedure.name}
                                </option>
                            ))}
                        </select>
                    </label>

                    <label>
                        Revision
                        <select value={historyId} onChange={(e) => selectRevision(e.target.value)} disabled={!revisions.length}>
                            <option value="">Select revision</option>
                            {revisions.map((rev) => (
                                <option key={rev.id} value={rev.id}>
                                    v{rev.versionNumber} · {rev.approvalStatus} {rev.lruName ? `(${rev.lruName})` : ""}
                                </option>
                            ))}
                        </select>
                    </label>

                    <label className="file-input">
                        Hardware fixture
                        <input type="file" accept=".csv,.json" onChange={(e) => setFile(e.target.files?.[0] || null)} />
                    </label>

                    <div className="validation-actions">
                        <select value={format} onChange={(e) => setFormat(e.target.value)} aria-label="Fixture format">
                            <option>CSV</option>
                            <option>JSON</option>
                        </select>
                        <button type="submit" disabled={busy || !historyId || !file}>
                            {busy ? "Processing…" : "Upload and validate"}
                        </button>
                    </div>
                </form>

                {/* Synthetic Fault Injection Controls (T13-B1, T13-B3) */}
                <div className="synthetic-controls">
                    <div className="synthetic-grid">
                        <label>
                            Fault category
                            <select
                                value={faultCategory}
                                onChange={(e) => setFaultCategory(e.target.value)}
                                disabled={injectionBusy || !historyId || !isRevisionApproved}
                            >
                                <option value="OUT_OF_RANGE">Out-of-range value (Numeric)</option>
                                <option value="DISCRETE_MISMATCH">Discrete signal mismatch (State/Behaviour)</option>
                                <option value="MISSING_SIGNAL">Missing signal / Dropped telemetry</option>
                            </select>
                        </label>

                        <label>
                            Target test step
                            <select
                                value={targetStepId}
                                onChange={(e) => setTargetStepId(e.target.value)}
                                disabled={injectionBusy || !historyId || !isRevisionApproved}
                            >
                                <option value="">Auto-target (first applicable step)</option>
                                {revisionSteps.map((step) => (
                                    <option key={step.id} value={step.id}>
                                        Step #{step.id || step.executionOrder}
                                        {step.expectedRange ? ` (${step.expectedRange} ${step.expectedUnit || ""})` : step.expectedBehaviour ? ` (${step.expectedBehaviour})` : ""}
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label>
                            Targeted LRU
                            <input
                                type="text"
                                readOnly
                                value={selectedRevision ? `${selectedRevision.lruName || "LRU"} (ID: ${selectedRevision.lruId})` : "Select a revision"}
                                style={{ background: "var(--surface)", border: "1px solid var(--border)", padding: "8px", borderRadius: "var(--radius)", color: "var(--muted)" }}
                            />
                        </label>
                    </div>

                    <div className="validation-actions">
                        <button
                            type="button"
                            className="button button--secondary"
                            disabled={injectionBusy || !historyId || !isRevisionApproved}
                            onClick={() => triggerSynthetic("NOMINAL")}
                        >
                            {injectionBusy ? "Simulating…" : "Run nominal simulation"}
                        </button>
                        <button
                            type="button"
                            className="button"
                            style={{ background: "#73481b", color: "#f7d0a1" }}
                            disabled={injectionBusy || !historyId || !isRevisionApproved}
                            onClick={() => triggerSynthetic("FAULT_INJECTED")}
                        >
                            {injectionBusy ? "Injecting fault…" : "Inject synthetic fault"}
                        </button>
                        {!isRevisionApproved && historyId ? (
                            <span className="badge badge--outlier" style={{ textTransform: "none", padding: "6px 12px" }}>
                                Synthetic injection requires an APPROVED revision. (Current: {selectedRevision?.approvalStatus})
                            </span>
                        ) : null}
                    </div>
                </div>

                {error ? <div className="alert-banner alert-banner--error">{error}</div> : null}
                {successMessage ? <div className="alert-banner alert-banner--success">{successMessage}</div> : null}
            </section>

            <section className="validation-layout">
                <div className="page-card">
                    <div className="section-heading">
                        <div>
                            <p className="eyebrow">Report history</p>
                            <h2 className="section-title">Revision runs</h2>
                        </div>
                        <span className="status-pill">{runs.length} runs</span>
                    </div>

                    {!historyId ? (
                        <p className="helper-text">Select a procedure revision to load its validation history.</p>
                    ) : !runs.length ? (
                        <p className="helper-text">No reports exist for this revision yet.</p>
                    ) : (
                        <div className="validation-run-list">
                            {runs.map((run) => {
                                const isSyn = run.sourceFilename?.toLowerCase().startsWith("synthetic-");
                                return (
                                    <button
                                        key={run.id}
                                        className={`validation-run ${selectedRun?.id === run.id ? "is-selected" : ""}`}
                                        onClick={() => selectRun(run)}
                                    >
                                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                                            <strong>Run #{run.id}</strong>
                                            <span className={`badge ${isSyn ? "badge--synthetic" : "badge--telemetry"}`}>
                                                {isSyn ? "SYNTHETIC" : "TELEMETRY"}
                                            </span>
                                        </div>
                                        <span>{run.sourceFilename}</span>
                                        <small>{run.status} · {run.uploadedAt ? new Date(run.uploadedAt).toLocaleString() : ""}</small>
                                    </button>
                                );
                            })}
                        </div>
                    )}
                </div>

                <div className="page-card">
                    <div className="section-heading">
                        <div>
                            <p className="eyebrow">Validation report & trace evidence</p>
                            <h2 className="section-title">{selectedRun ? selectedRun.sourceFilename : "No report selected"}</h2>
                        </div>
                        {selectedRun ? <span className="status-pill">{selectedRun.status}</span> : null}
                    </div>

                    {selectedRun ? (
                        <>
                            {/* Evidence Labeling Banner (T13-B1, T13-B2) */}
                            {isSyntheticEvidence ? (
                                <div className="evidence-banner evidence-banner--synthetic">
                                    <span className="badge badge--synthetic">SYNTHETIC EVIDENCE</span>
                                    <span>Injected simulation evidence generated against immutable snapshot. Never represent as actual flight hardware measurement.</span>
                                </div>
                            ) : (
                                <div className="evidence-banner evidence-banner--telemetry">
                                    <span className="badge badge--telemetry">HARDWARE TELEMETRY</span>
                                    <span>External telemetry fixture uploaded and compared against procedure requirements.</span>
                                </div>
                            )}

                            {/* Traceability Metadata Grid */}
                            <div className="report-traceability">
                                <div>
                                    <span>Procedure Revision</span>
                                    <strong>{reportData?.procedureName || selectedRevision?.name || "Procedure"} (v{reportData?.procedureVersionNumber || selectedRevision?.versionNumber || 1})</strong>
                                </div>
                                <div>
                                    <span>Target LRU</span>
                                    <strong>{reportData?.lruName || selectedRevision?.lruName || "LRU-" + (selectedRevision?.lruId || "N/A")}</strong>
                                </div>
                                <div>
                                    <span>Recorded By</span>
                                    <strong>{reportData?.recordedBy || selectedRun.recordedBy || "SYSTEM"}</strong>
                                </div>
                                <div>
                                    <span>Source Format</span>
                                    <strong>{reportData?.sourceFormat || selectedRun.sourceFormat || "JSON"}</strong>
                                </div>
                            </div>

                            {/* Summary Metrics */}
                            <div className="validation-metrics">
                                <Metric label="Matched" value={selectedRun.matchedCount} tone="good" />
                                <Metric label="Mismatched" value={selectedRun.mismatchedCount} tone="bad" />
                                <Metric label="Outliers" value={selectedRun.outlierCount} tone="warn" />
                                <Metric label="Missing" value={selectedRun.missingCount} tone="bad" />
                                <Metric label="Pass Rate" value={`${passRate}%`} tone="info" />
                            </div>

                            {/* Discrepancies and Gap Remediation Loop */}
                            <div className="validation-remediation">
                                <div className="section-heading">
                                    <h3 className="section-title">Gap remediation loop</h3>
                                    <span className="helper-text">{addressed.size}/{issues.length} addressed</span>
                                </div>

                                {!issues.length ? (
                                    <p className="helper-text" style={{ color: "#8bc7a5" }}>
                                        ✓ No discrepancies found. All readings matched procedure expectations.
                                    </p>
                                ) : (
                                    issues.map((reading) => (
                                        <article className={`validation-issue ${addressed.has(reading.id) ? "is-addressed" : ""}`} key={reading.id}>
                                            <div>
                                                <div style={{ display: "flex", gap: "8px", alignItems: "center", marginBottom: "4px" }}>
                                                    <span className={`badge badge--${reading.result.toLowerCase()}`}>
                                                        {resultLabel[reading.result] || reading.result}
                                                    </span>
                                                    <strong>Test step #{reading.testStepId}</strong>
                                                </div>
                                                <p>{reading.errorMessage || "Observed value differed from expected specification."}</p>
                                                {reading.observedValue != null ? (
                                                    <small style={{ display: "block" }}>
                                                        Observed: {reading.observedValue} {reading.observedUnit || ""}
                                                        {reading.deviation != null ? ` · Deviation: ${reading.deviation}` : ""}
                                                    </small>
                                                ) : reading.observedBehaviour ? (
                                                    <small style={{ display: "block" }}>Observed behaviour: {reading.observedBehaviour}</small>
                                                ) : null}
                                            </div>
                                            <button onClick={() => markAddressed(reading)} disabled={addressed.has(reading.id)}>
                                                {addressed.has(reading.id) ? "Addressed" : "Record remediation"}
                                            </button>
                                        </article>
                                    ))
                                )}

                                <div className="validation-actions" style={{ marginTop: "16px" }}>
                                    <Link className="button button--secondary" to="/procedures">Open procedure builder</Link>
                                    <Link className="button button--secondary" to="/procedures/review-queue">Open review queue</Link>
                                </div>
                            </div>
                        </>
                    ) : (
                        <p className="helper-text">Select a report to view evidence, discrepancies, and remediation status.</p>
                    )}
                </div>
            </section>
        </ManagementLayout>
    );
}

function Metric({ label, value, tone }) {
    return (
        <div className={`validation-metric validation-metric--${tone}`}>
            <span>{label}</span>
            <strong>{value ?? 0}</strong>
        </div>
    );
}
