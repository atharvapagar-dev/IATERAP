import { useEffect, useState } from "react";
import ManagementLayout from "../components/ManagementLayout";
import api from "../services/api";

export default function AiSuggestionQueuePage() {
    const [items, setItems] = useState([]);
    const [message, setMessage] = useState("");
    const [loading, setLoading] = useState(true);
    const [prompt, setPrompt] = useState("");
    const [procedureId, setProcedureId] = useState("");
    const [stepId, setStepId] = useState("");
    const load = () => api.get("/ai/suggestions/queue").then((r) => setItems(r.data)).catch(() => setMessage("Unable to load your suggestion queue.")).finally(() => setLoading(false));
    useEffect(() => { load(); }, []);
    const requestSuggestion = async (event) => {
        event.preventDefault();
        if (!prompt.trim()) return;
        try {
            const response = await api.post("/ai/suggestions/queue", { prompt, test_procedure_id: procedureId ? Number(procedureId) : null, test_step_id: stepId ? Number(stepId) : null });
            setItems((current) => [response.data, ...current]); setPrompt(""); setMessage("Suggestion added to your private review queue.");
        } catch (error) { setMessage(error.response?.data?.message || "Unable to request a suggestion."); }
    };
    const decide = async (id, action) => {
        setMessage("");
        try { await api.post(`/ai/suggestions/${id}/${action}`); setItems((current) => current.filter((item) => item.id !== id)); setMessage(`Suggestion ${action}ed and recorded under your identity.`); }
        catch (error) { setMessage(error.response?.data?.message || "Unable to record that decision."); }
    };
    return <ManagementLayout title="AI suggestion queue" description="Review advisory output before it can be attributed to you. Nothing is applied automatically.">
        {message ? <section className="page-card"><p className="helper-text">{message}</p></section> : null}
        <section className="page-card">
            <form onSubmit={requestSuggestion} style={{ display: "grid", gap: 10, marginBottom: 20 }}>
                <label>What should the advisory service help draft?<textarea value={prompt} onChange={(event) => setPrompt(event.target.value)} maxLength="2000" required /></label>
                <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}><label>Procedure ID <input type="number" value={procedureId} onChange={(event) => setProcedureId(event.target.value)} /></label><label>Step ID <input type="number" value={stepId} onChange={(event) => setStepId(event.target.value)} /></label><button type="submit">Request suggestion</button></div>
            </form>
            <div className="section-heading"><div><p className="eyebrow">Engineer decision required</p><h2 className="section-title">Pending suggestions</h2></div></div>
            {loading ? <p className="helper-text">Loading suggestions...</p> : null}
            {!loading && !items.length ? <p className="helper-text">No pending AI suggestions. Generate one from the advisory endpoint when you need help drafting a test update.</p> : null}
            {items.map((item) => <article key={item.id} className="page-card" style={{ margin: "12px 0 0" }}>
                <p className="eyebrow">{item.source || "AI advisory"} · {Math.round(item.confidence * 100)}% confidence</p>
                <p style={{ whiteSpace: "pre-wrap" }}>{item.suggestion}</p>
                <p className="helper-text">Procedure {item.testProcedureId ?? "—"} · Step {item.testStepId ?? "—"} · queued {new Date(item.createdAt).toLocaleString()}</p>
                <div style={{ display: "flex", gap: 10 }}><button onClick={() => decide(item.id, "accept")}>Accept</button><button className="button-secondary" onClick={() => decide(item.id, "dismiss")}>Dismiss</button></div>
            </article>)}
        </section>
    </ManagementLayout>;
}
