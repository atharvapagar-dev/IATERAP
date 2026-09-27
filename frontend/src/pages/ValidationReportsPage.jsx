import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import ManagementLayout from "../components/ManagementLayout";
import { validationService } from "../services/validationService";
import "../assets/styles/ValidationReports.css";

const resultLabel = { MATCH: "Matched", MISMATCH: "Mismatched", OUTLIER: "Outlier", MISSING: "Missing" };

export default function ValidationReportsPage() {
    const [historyId, setHistoryId] = useState("");
    const [runs, setRuns] = useState([]);
    const [selectedRun, setSelectedRun] = useState(null);
    const [readings, setReadings] = useState([]);
    const [file, setFile] = useState(null);
    const [format, setFormat] = useState("CSV");
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const [addressed, setAddressed] = useState(() => new Set());

    const loadRuns = async () => {
        if (!historyId) return;
        setError("");
        try {
            const response = await validationService.listRuns(historyId);
            setRuns(response.data || []);
            if (response.data?.length) await selectRun(response.data[0]);
        } catch (err) { setError(err.response?.data?.message || "Unable to load validation reports."); }
    };

    const selectRun = async (run) => {
        setSelectedRun(run);
        try { setReadings((await validationService.getReadings(run.id)).data || []); }
        catch (err) { setError("Unable to load report readings."); }
    };

    const upload = async (event) => {
        event.preventDefault();
        if (!historyId || !file) { setError("Enter a procedure revision ID and choose a CSV or JSON fixture."); return; }
        setBusy(true); setError("");
        try {
            const response = await validationService.upload(historyId, format, file);
            setRuns((current) => [response.data, ...current]);
            await selectRun(response.data);
            setFile(null); event.target.reset();
        } catch (err) { setError(err.response?.data?.message || "Fixture upload failed. Check the revision and file format."); }
        finally { setBusy(false); }
    };

    const issues = useMemo(() => readings.filter((reading) => reading.result !== "MATCH"), [readings]);
    const markAddressed = (id) => setAddressed((current) => new Set([...current, id]));
    const count = (key) => selectedRun?.[key] ?? 0;

    return <ManagementLayout title="Validation reports" description="Compare externally supplied evidence with an immutable procedure revision and close gaps before approval.">
        <section className="page-card validation-toolbar">
            <div><p className="eyebrow">Evidence ingestion</p><h2 className="section-title">Run an independent validation report</h2></div>
            <form onSubmit={upload} className="validation-upload-form">
                <label>Procedure revision ID<input value={historyId} onChange={(e) => setHistoryId(e.target.value)} type="number" min="1" placeholder="e.g. 42" /></label>
                <label>Fixture format<select value={format} onChange={(e) => setFormat(e.target.value)}><option>CSV</option><option>JSON</option></select></label>
                <label className="file-input">Evidence fixture<input type="file" accept=".csv,.json" onChange={(e) => setFile(e.target.files?.[0] || null)} /></label>
                <div className="validation-actions"><button type="submit" disabled={busy}>{busy ? "Processing…" : "Upload and validate"}</button><button type="button" className="button button--secondary" onClick={loadRuns}>Load reports</button></div>
            </form>
            {error ? <p className="coverage-summary__error">{error}</p> : null}
        </section>
        <section className="validation-layout">
            <div className="page-card"><div className="section-heading"><div><p className="eyebrow">Report history</p><h2 className="section-title">Revision runs</h2></div><span className="status-pill">{runs.length} runs</span></div>
                {!runs.length ? <p className="helper-text">Enter a revision ID to load reports. Uploads remain tied to the selected immutable revision.</p> : <div className="validation-run-list">{runs.map((run) => <button key={run.id} className={`validation-run ${selectedRun?.id === run.id ? "is-selected" : ""}`} onClick={() => selectRun(run)}><strong>Run #{run.id}</strong><span>{run.sourceFilename}</span><small>{run.status} · {run.uploadedAt ? new Date(run.uploadedAt).toLocaleString() : ""}</small></button>)}</div>}
            </div>
            <div className="page-card"><div className="section-heading"><div><p className="eyebrow">Validation report</p><h2 className="section-title">{selectedRun ? selectedRun.sourceFilename : "No report selected"}</h2></div>{selectedRun ? <span className="status-pill">{selectedRun.status}</span> : null}</div>
                {selectedRun ? <><div className="validation-metrics"><Metric label="Matched" value={count("matchedCount")} tone="good" /><Metric label="Mismatched" value={count("mismatchedCount")} tone="bad" /><Metric label="Outliers" value={count("outlierCount")} tone="warn" /><Metric label="Missing" value={count("missingCount")} tone="bad" /></div><div className="validation-remediation"><div className="section-heading"><h3 className="section-title">Gap remediation loop</h3><span className="helper-text">{addressed.size}/{issues.length} addressed</span></div>{!issues.length ? <p className="helper-text">No discrepancies found. This revision is ready for reviewer approval.</p> : issues.map((reading) => <article className={`validation-issue ${addressed.has(reading.id) ? "is-addressed" : ""}`} key={reading.id}><div><strong>{resultLabel[reading.result] || reading.result} · Test step #{reading.testStepId}</strong><p>{reading.errorMessage || "Review the observed value against the expected contract."}</p>{reading.deviation != null ? <small>Deviation: {reading.deviation}</small> : null}</div><button onClick={() => markAddressed(reading.id)} disabled={addressed.has(reading.id)}>{addressed.has(reading.id) ? "Addressed" : "Mark addressed"}</button></article>)}<Link className="button button--secondary" to="/procedures">Open procedure builder</Link></div></> : <p className="helper-text">Select a run to view evidence, discrepancies, and remediation status.</p>}
            </div>
        </section>
    </ManagementLayout>;
}

function Metric({ label, value, tone }) { return <div className={`validation-metric validation-metric--${tone}`}><span>{label}</span><strong>{value}</strong></div>; }
