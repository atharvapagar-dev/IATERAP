import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import { signalLibraryService } from "../services/signalLibraryService";
import { signalRangeService } from "../services/signalRangeService";

export default function SignalLibraryDetailPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [signalLibrary, setSignalLibrary] = useState(null);
    const [signalRanges, setSignalRanges] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const loadSignalLibrary = async () => {
            try {
                const [libraryResponse, rangesResponse] = await Promise.all([
                    signalLibraryService.getSignalLibraryById(id),
                    signalRangeService.getSignalRangesBySignalLibraryId(id),
                ]);

                setSignalLibrary(libraryResponse.data);
                setSignalRanges(rangesResponse.data);
            } catch (fetchError) {
                console.error("Failed to fetch signal library detail:", fetchError);
                setError("Unable to load the selected signal library.");
            } finally {
                setLoading(false);
            }
        };

        loadSignalLibrary();
    }, [id]);

    return (
        <ManagementLayout
            title="Signal Library Details"
            description="Review the mapped interface, LRU, and any nested signal ranges tied to this library."
        >
            <div className="page-toolbar">
                <button
                    className="button--secondary"
                    onClick={() => navigate("/signal-libraries")}
                >
                    Back to list
                </button>

                <button onClick={() => navigate(`/signal-libraries/${id}/edit`)}>
                    Edit signal library
                </button>
            </div>

            {loading ? (
                <p className="helper-text">Loading signal library…</p>
            ) : error ? (
                <div className="empty-state">{error}</div>
            ) : signalLibrary ? (
                <div className="stack">
                    <section className="page-card">
                        <div className="detail-grid">
                            <div className="detail-item">
                                <p className="detail-item__label">Name</p>
                                <p className="detail-item__value">
                                    {signalLibrary.name}
                                </p>
                            </div>

                            <div className="detail-item">
                                <p className="detail-item__label">
                                    Engineering Unit
                                </p>
                                <p className="detail-item__value">
                                    {signalLibrary.engineeringUnit || "—"}
                                </p>
                            </div>

                            <div className="detail-item">
                                <p className="detail-item__label">Interface</p>
                                <p className="detail-item__value">
                                    {signalLibrary.interfaceEntity?.name || "—"}
                                </p>
                            </div>

                            <div className="detail-item">
                                <p className="detail-item__label">LRU</p>
                                <p className="detail-item__value">
                                    {signalLibrary.lru?.name || "—"}
                                </p>
                            </div>
                        </div>

                        <div style={{ marginTop: "18px" }}>
                            <p className="detail-item__label">Description</p>
                            <p className="helper-text">
                                {signalLibrary.description ||
                                    "No description provided."}
                            </p>
                        </div>
                    </section>

                    <section className="page-card stack">
                        <h2 className="section-title">Signal Ranges</h2>

                        {signalRanges.length ? (
                            <table className="entity-table">
                                <thead>
                                    <tr>
                                        <th>Name</th>
                                        <th>Minimum</th>
                                        <th>Maximum</th>
                                        <th>Severity</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {signalRanges.map((range) => (
                                        <tr key={range.id}>
                                            <td>{range.name}</td>
                                            <td>{range.minimumValue}</td>
                                            <td>{range.maximumValue}</td>
                                            <td>{range.severity || "—"}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        ) : (
                            <div className="empty-state">
                                No signal ranges are linked to this signal
                                library yet.
                            </div>
                        )}
                    </section>
                </div>
            ) : null}
        </ManagementLayout>
    );
}
