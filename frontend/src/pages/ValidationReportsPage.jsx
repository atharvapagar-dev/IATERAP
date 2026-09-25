import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import ManagementLayout from "../components/ManagementLayout";
import { validationService } from "../services/validationService";
import { testProcedureService } from "../services/testProcedureService";
import { reviewerCommentService } from "../services/reviewerCommentService";
import "../assets/styles/ValidationReports.css";

const resultLabel = { MATCH: "Matched", MISMATCH: "Mismatched", OUTLIER: "Outlier", MISSING: "Missing" };

export default function ValidationReportsPage() {
    const [procedures, setProcedures] = useState([]); const [revisions, setRevisions] = useState([]);
    const [procedureId, setProcedureId] = useState(""); const [historyId, setHistoryId] = useState("");
    const [runs, setRuns] = useState([]); const [selectedRun, setSelectedRun] = useState(null); const [readings, setReadings] = useState([]);
    const [file, setFile] = useState(null); const [format, setFormat] = useState("CSV"); const [busy, setBusy] = useState(false); const [error, setError] = useState("");
    const [addressed, setAddressed] = useState(() => new Set());

    useEffect(() => { testProcedureService.getAllProcedures().then((r) => setProcedures(r.data || [])).catch(() => setError("Unable to load procedures.")); }, []);

    const selectProcedure = async (value) => {
        setProcedureId(value); setHistoryId(""); setRevisions([]); setRuns([]); setSelectedRun(null); setReadings([]);
        if (!value) return;
        try { setRevisions((await testProcedureService.getHistory(value)).data || []); } catch (err) { setError("Unable to load procedure revisions."); }
    };
    const selectRevision = async (value) => { setHistoryId(value); setSelectedRun(null); setReadings([]); setAddressed(new Set()); if (value) await loadRuns(value); };
    const loadRuns = async (revision = historyId) => {
        if (!revision) return; setError("");
        try { const response = await validationService.listRuns(revision); setRuns(response.data || []); if (response.data?.length) await selectRun(response.data[0]); }
        catch (err) { setError(err.response?.data?.message || "Unable to load validation reports."); }
    };
    const selectRun = async (run) => { setSelectedRun(run); try { setReadings((await validationService.getReadings(run.id)).data || []); } catch (err) { setError("Unable to load report readings."); } };
    const upload = async (event) => {
        event.preventDefault();
        if (!historyId || !file) { setError("Select a procedure revision and choose a CSV or JSON fixture."); return; }
        setBusy(true); setError("");
        try { const response = await validationService.upload(historyId, format, file); setRuns((current) => [response.data, ...current]); await selectRun(response.data); setFile(null); event.target.reset(); }
        catch (err) { setError(err.response?.data?.message || "Fixture upload failed. Check the revision and file format."); } finally { setBusy(false); }
    };
    const issues = useMemo(() => readings.filter((reading) => reading.result !== "MATCH"), [readings]);
    const markAddressed = async (reading) => {
        try {
            await reviewerCommentService.createComment({ procedureHistoryId: Number(historyId), testStepId: reading.testStepId, commentText: `Validation remediation recorded for ${resultLabel[reading.result] || reading.result} reading from ${selectedRun.sourceFilename}. ${reading.errorMessage || "Review evidence and update the procedure if required."}` });
            setAddressed((current) => new Set([...current, reading.id]));
        } catch (err) { setError("Unable to persist the remediation note."); }
    };

    return <ManagementLayout title="Validation reports" description="Compare external evidence with an immutable procedure revision and close gaps before approval.">
        <section className="page-card validation-toolbar"><div><p className="eyebrow">Evidence ingestion</p><h2 className="section-title">Run an independent validation report</h2></div>
            <form onSubmit={upload} className="validation-upload-form">
                <label>Procedure<select value={procedureId} onChange={(e) => selectProcedure(e.target.value)}><option value="">Select procedure</option>{procedures.map((procedure) => <option key={procedure.id} value={procedure.id}>{procedure.name}</option>)}</select></label>
                <label>Revision<select value={historyId} onChange={(e) => selectRevision(e.target.value)} disabled={!revisions.length}><option value="">Select revision</option>{revisions.map((revision) => <option key={revision.id} value={revision.id}>v{revision.versionNumber} · {revision.approvalStatus}</option>)}</select></label>
                <label className="file-input">Evidence fixture<input type="file" accept=".csv,.json" onChange={(e) => setFile(e.target.files?.[0] || null)} /></label>
                <div className="validation-actions"><select value={format} onChange={(e) => setFormat(e.target.value)} aria-label="Fixture format"><option>CSV</option><option>JSON</option></select><button type="submit" disabled={busy}>{busy ? "Processing…" : "Upload and validate"}</button></div>
            </form>{error ? <p className="coverage-summary__error">{error}</p> : null}
        </section>
        <section className="validation-layout"><div className="page-card"><div className="section-heading"><div><p className="eyebrow">Report history</p><h2 className="section-title">Revision runs</h2></div><span className="status-pill">{runs.length} runs</span></div>{!historyId ? <p className="helper-text">Select a procedure revision to load its validation history.</p> : !runs.length ? <p className="helper-text">No reports exist for this revision yet.</p> : <div className="validation-run-list">{runs.map((run) => <button key={run.id} className={`validation-run ${selectedRun?.id === run.id ? "is-selected" : ""}`} onClick={() => selectRun(run)}><strong>Run #{run.id}</strong><span>{run.sourceFilename}</span><small>{run.status} · {run.uploadedAt ? new Date(run.uploadedAt).toLocaleString() : ""}</small></button>)}</div>}</div>
            <div className="page-card"><div className="section-heading"><div><p className="eyebrow">Validation report</p><h2 className="section-title">{selectedRun ? selectedRun.sourceFilename : "No report selected"}</h2></div>{selectedRun ? <span className="status-pill">{selectedRun.status}</span> : null}</div>{selectedRun ? <><div className="validation-metrics"><Metric label="Matched" value={selectedRun.matchedCount} tone="good" /><Metric label="Mismatched" value={selectedRun.mismatchedCount} tone="bad" /><Metric label="Outliers" value={selectedRun.outlierCount} tone="warn" /><Metric label="Missing" value={selectedRun.missingCount} tone="bad" /></div><div className="validation-remediation"><div className="section-heading"><h3 className="section-title">Gap remediation loop</h3><span className="helper-text">{addressed.size}/{issues.length} addressed</span></div>{!issues.length ? <p className="helper-text">No discrepancies found. This revision is ready for reviewer approval.</p> : issues.map((reading) => <article className={`validation-issue ${addressed.has(reading.id) ? "is-addressed" : ""}`} key={reading.id}><div><strong>{resultLabel[reading.result] || reading.result} · Test step #{reading.testStepId}</strong><p>{reading.errorMessage || "Review the observed value against the expected contract."}</p>{reading.deviation != null ? <small>Deviation: {reading.deviation}</small> : null}</div><button onClick={() => markAddressed(reading)} disabled={addressed.has(reading.id)}>{addressed.has(reading.id) ? "Addressed" : "Record remediation"}</button></article>)}<div className="validation-actions"><Link className="button button--secondary" to="/procedures">Open procedure builder</Link><Link className="button button--secondary" to="/procedures/review-queue">Open review queue</Link></div></div></> : <p className="helper-text">Select a report to view evidence, discrepancies, and remediation status.</p>}</div>
        </section>
    </ManagementLayout>;
}
function Metric({ label, value, tone }) { return <div className={`validation-metric validation-metric--${tone}`}><span>{label}</span><strong>{value ?? 0}</strong></div>; }
