import { useEffect, useState } from "react";
import ManagementLayout from "../components/ManagementLayout";
import api from "../services/api";
import { aircraftService } from "../services/aircraftService";
import { syntheticEventService } from "../services/syntheticEventService";

const trendLabel = {
    RISING: "Rising findings",
    FALLING: "Improving",
    STABLE: "Stable",
    INSUFFICIENT_DATA: "More evidence needed",
};

function Metric({ label, value, emphasis = false }) {
    return <article className="page-card" style={{ margin: 0, minWidth: 150, flex: "1 1 150px" }}>
        <p className="eyebrow">{label}</p>
        <p style={{ fontSize: "1.7rem", fontWeight: 700, margin: "0.35rem 0 0", color: emphasis ? "#d55" : undefined }}>{value}</p>
    </article>;
}

export default function ReliabilityDashboardPage() {
    const [dashboard, setDashboard] = useState(null);
    const [aircraft, setAircraft] = useState([]);
    const [aircraftId, setAircraftId] = useState("");
    const [generationMessage, setGenerationMessage] = useState("");
    const [generating, setGenerating] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        api.get("/reliability/dashboard").then((response) => setDashboard(response.data))
            .catch((requestError) => {
                setError(requestError.response?.status === 403
                    ? "Reliability analytics is available to reliability engineers, system engineers, and administrators."
                    : "Unable to load reliability analytics.");
            });
        aircraftService.getAllAircraft().then((response) => {
            setAircraft(response.data);
            if (response.data.length) setAircraftId(String(response.data[0].id));
        }).catch(() => setGenerationMessage("Unable to load aircraft for synthetic event generation."));
    }, []);

    const generateEvents = async () => {
        if (!aircraftId) return;
        setGenerating(true);
        setGenerationMessage("");
        try {
            const response = await syntheticEventService.generate(aircraftId);
            const result = response.data;
            setGenerationMessage(`Generated ${result.eventsCreated} synthetic event(s); ${result.eventsAlreadyPresent} already existed.`);
            const dashboardResponse = await api.get("/reliability/dashboard");
            setDashboard(dashboardResponse.data);
        } catch (requestError) {
            setGenerationMessage(requestError.response?.data?.message || "Unable to generate synthetic events.");
        } finally {
            setGenerating(false);
        }
    };

    return <ManagementLayout
        title="Reliability analytics"
        description="Evidence-only trends from persisted external validation and synthetic fault-injection runs."
    >
        {error ? <section className="page-card"><p className="coverage-summary__error">{error}</p></section> : null}
        <section className="page-card">
            <div className="section-heading"><div><p className="eyebrow">Synthetic fleet evidence</p><h2 className="section-title">Generate event log</h2></div></div>
            <p className="helper-text">Creates clearly labelled simulated sortie, failure, and repair events from persisted validation outcomes. It never imports live aircraft telemetry.</p>
            <div style={{ display: "flex", gap: 10, alignItems: "center", flexWrap: "wrap" }}>
                <select value={aircraftId} onChange={(event) => setAircraftId(event.target.value)} aria-label="Aircraft for synthetic events">
                    <option value="">Select aircraft</option>
                    {aircraft.map((item) => <option key={item.id} value={item.id}>{item.name} {item.manufacturer ? `(${item.manufacturer})` : ""}</option>)}
                </select>
                <button onClick={generateEvents} disabled={!aircraftId || generating}>{generating ? "Generating…" : "Generate synthetic events"}</button>
            </div>
            {generationMessage ? <p className="helper-text" style={{ marginTop: 10 }}>{generationMessage}</p> : null}
        </section>
        {!dashboard && !error ? <section className="page-card"><p className="helper-text">Loading reliability evidence...</p></section> : null}
        {dashboard ? <>
            <section style={{ display: "flex", gap: 12, flexWrap: "wrap", marginBottom: 18 }}>
                <Metric label="Validation runs" value={dashboard.totalRuns} />
                <Metric label="Readings compared" value={dashboard.totalReadings} />
                <Metric label="Findings" value={dashboard.findings} emphasis={dashboard.findings > 0} />
                <Metric label="Finding rate" value={`${dashboard.findingRate}%`} emphasis={dashboard.findingRate > 0} />
            </section>
            <section className="page-card">
                <div className="section-heading"><div><p className="eyebrow">Reliability trend</p><h2 className="section-title">Recurring evidence by LRU</h2></div><span className="helper-text">A finding is a mismatch, outlier, or missing reading.</span></div>
                {!dashboard.lrus?.length ? <p className="helper-text">No validation evidence has been recorded yet. Upload a CSV/JSON fixture or run a synthetic scenario against an approved procedure revision.</p> :
                    <table className="coverage-table"><thead><tr><th>LRU</th><th>Runs</th><th>Findings</th><th>Rate</th><th>Breakdown</th><th>Trend</th><th>Evidence</th></tr></thead><tbody>
                        {dashboard.lrus.map((lru) => <tr key={lru.lruId}><td>{lru.lruName}</td><td>{lru.totalRuns}</td><td>{lru.findings}</td><td>{lru.findingRate}%</td><td>{lru.mismatches} mismatch · {lru.outliers} outlier · {lru.missing} missing</td><td><span className="status-pill">{trendLabel[lru.trend] || lru.trend}</span></td><td>{lru.evidenceSources.join(" + ")}</td></tr>)}
                    </tbody></table>}
            </section>
            <p className="helper-text">Trend compares the earlier and more recent halves of an LRU’s evidence history. At least four runs are required before a direction is shown.</p>
        </> : null}
    </ManagementLayout>;
}
