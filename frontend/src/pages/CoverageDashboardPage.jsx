import { useEffect, useState } from "react";
import ManagementLayout from "../components/ManagementLayout";
import api from "../services/api";

const labels = {
    "COVERAGE-INTERFACE": "Interface",
    "COVERAGE-SIGNAL": "Signal",
    "COVERAGE-REQUIREMENT": "Requirement",
    "COVERAGE-FUNCTIONAL": "Functional",
    "COVERAGE-FAULT": "Fault",
    "COVERAGE-FMEA": "FMEA",
};

function score(value) { return value == null ? "Unavailable" : `${Number(value).toFixed(1)}%`; }

export default function CoverageDashboardPage() {
    const [rows, setRows] = useState([]);
    const [error, setError] = useState("");

    useEffect(() => {
        api.get("/coverage/heatmap")
            .then((response) => setRows(response.data))
            .catch(() => setError("Unable to load the coverage heatmap."));
    }, []);

    return (
        <ManagementLayout title="Coverage dashboard" description="Subsystem-level coverage aggregation and gap visibility.">
            <section className="page-card">
                <div className="section-heading">
                    <div><p className="eyebrow">Coverage heatmap</p><h2 className="section-title">Procedure coverage by LRU</h2></div>
                    <span className="status-pill">{rows.length} procedures</span>
                </div>
                {error ? <p className="coverage-summary__error">{error}</p> : null}
                <div className="coverage-table-wrap">
                    <table className="coverage-table">
                        <thead><tr><th>Procedure</th><th>LRU / subsystem</th>{Object.keys(labels).map((key) => <th key={key}>{labels[key]}</th>)}<th>Composite</th></tr></thead>
                        <tbody>{rows.map((row) => <tr key={row.procedureId}>
                            <td>{row.procedureName}</td><td>{row.lruName}</td>
                            {Object.keys(labels).map((key) => <td key={key} className={row.categories?.[key] == null ? "coverage-cell coverage-cell--na" : row.categories[key] < 70 ? "coverage-cell coverage-cell--low" : "coverage-cell coverage-cell--good"}>{score(row.categories?.[key])}</td>)}
                            <td><strong>{score(row.compositeScore)}</strong></td>
                        </tr>)}</tbody>
                    </table>
                    {!rows.length && !error ? <p className="helper-text">No evaluated procedures are available yet.</p> : null}
                </div>
            </section>
        </ManagementLayout>
    );
}
