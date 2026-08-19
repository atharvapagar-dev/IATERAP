import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import { testProcedureService } from "../services/testProcedureService";
import { getLrus } from "../services/lruService";

export default function TestProcedureListPage() {
    const navigate = useNavigate();
    const [procedures, setProcedures] = useState([]);
    const [lrus, setLrus] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const loadProcedures = async () => {
            try {
                setError("");
                const [procRes, lruRes] = await Promise.all([
                    testProcedureService.getAllProcedures(),
                    getLrus(),
                ]);
                setProcedures(procRes.data);
                setLrus(lruRes.data);
            } catch (fetchError) {
                console.error("Failed to fetch test procedures:", fetchError);
                setError("Unable to load test procedures right now.");
            } finally {
                setLoading(false);
            }
        };

        void loadProcedures();
    }, []);

    const lruName = (lruId) => lrus.find((l) => l.id === lruId)?.name || "-";

    return (
        <ManagementLayout
            title="Test Procedure Management"
            description="Build and maintain Test Procedures, each composed of ordered Test Steps bound to a single LRU."
        >
            <div className="page-toolbar">
                <p className="helper-text">
                    Select a procedure to open the Procedure Builder, or create a new one.
                </p>

                <button onClick={() => navigate("/procedures/create")}>
                    Create Test Procedure
                </button>
            </div>

            <section className="page-card stack">
                {error ? <p style={{ color: "var(--danger)" }}>{error}</p> : null}

                {loading ? (
                    <p className="helper-text">Loading test procedures...</p>
                ) : !procedures.length ? (
                    <div className="empty-state">
                        No test procedures yet. Create one to start building Test Steps against an LRU.
                    </div>
                ) : (
                    <table className="entity-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>LRU</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {procedures.map((item) => (
                                <tr key={item.id}>
                                    <td>{item.id}</td>
                                    <td><strong>{item.name}</strong></td>
                                    <td>{lruName(item.lruId)}</td>
                                    <td>{item.approvalStatus}</td>
                                    <td>
                                        <div className="table-actions">
                                            <button onClick={() => navigate(`/procedures/${item.id}/builder`)}>
                                                Open Builder
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </section>
        </ManagementLayout>
    );
}