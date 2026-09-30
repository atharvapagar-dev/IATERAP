import { useEffect, useState } from "react";
import ManagementLayout from "../components/ManagementLayout";
import api from "../services/api";

const labels = { COVERED: "Covered", PARTIALLY_COVERED: "Partially Covered", NOT_COVERED: "Not Covered" };

export default function CoverageDashboardPage() {
    const [rows, setRows] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        api.get("/coverage/heatmap").then((response) => setRows(response.data))
            .catch(() => setError("Unable to load the coverage heatmap.")).finally(() => setLoading(false));
    }, []);

    return <ManagementLayout title="Coverage dashboard" description="Subsystem-level coverage aggregation and gap visibility."><section className="page-card">
        <div className="section-heading"><div><p className="eyebrow">Coverage heatmap</p><h2 className="section-title">LRU coverage by subsystem</h2></div><span className="status-pill">{rows.reduce((total, row) => total + row.procedures.length, 0)} procedures</span></div>
        <div className="coverage-legend" aria-label="Coverage status legend">{Object.entries(labels).map(([status, label]) => <span className={`coverage-status coverage-status--${status.toLowerCase()}`} key={status}>{label}</span>)}</div>
        {loading ? <p className="helper-text">Loading coverage heatmap...</p> : null}
        {error ? <p className="coverage-summary__error">{error}</p> : null}
        {!loading && !error && !rows.length ? <p className="helper-text">No evaluated procedures are available yet.</p> : null}
        {!loading && !error ? rows.map((row) => <section className="coverage-subsystem" key={row.subsystemName}><div className="section-heading"><h3>{row.subsystemName}</h3><span className="helper-text">{row.coveredCount} covered · {row.partiallyCoveredCount} partial · {row.notCoveredCount} not covered</span></div><table className="coverage-table"><thead><tr><th>Procedure</th><th>LRU</th><th>Status</th></tr></thead><tbody>{row.procedures.map((procedure) => <tr key={procedure.procedureId}><td>{procedure.procedureName}</td><td>{procedure.lruName}</td><td><span className={`coverage-status coverage-status--${procedure.status.toLowerCase()}`}>{labels[procedure.status]}</span></td></tr>)}</tbody></table></section>) : null}
    </section></ManagementLayout>;
}
