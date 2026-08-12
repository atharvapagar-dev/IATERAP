import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import { testProcedureService } from "../services/testProcedureService";

export default function ProcedureListPage() {
    const navigate = useNavigate();
    const [procedures, setProcedures] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const loadProcedures = async () => {
            try {
                setError("");
                const response = await testProcedureService.getAllProcedures();
                setProcedures(response.data);
            } catch (fetchError) {
                console.error("Failed to fetch procedures:", fetchError);
                setError("Unable to load procedures right now.");
            } finally {
                setLoading(false);
            }
        };

        void loadProcedures();
    }, []);

    return (
        <ManagementLayout
            title="Test Procedures"
            description="Author and version Test Procedures from typed Test Steps."
        >
            <div className="page-toolbar">
                <p className="helper-text">
                    Open an existing procedure to edit it in the Procedure
                    Builder, or start a new one.
                </p>

                <button onClick={() => navigate("/procedures/new")}>
                    New Procedure
                </button>
            </div>

            <section className="page-card stack">
                {error ? <p style={{ color: "var(--danger)" }}>{error}</p> : null}

                {loading ? (
                    <p className="helper-text">Loading procedures…</p>
                ) : procedures.length === 0 ? (
                    <p className="helper-text">No procedures yet.</p>
                ) : (
                    <table>
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>Approval Status</th>
                                <th>Version</th>
                                <th># Steps</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            {procedures.map((p) => (
                                <tr key={p.id}>
                                    <td>{p.name}</td>
                                    <td>{p.approvalStatus}</td>
                                    <td>{p.versionNumber}</td>
                                    <td>{p.steps?.length ?? 0}</td>
                                    <td>
                                        <button onClick={() => navigate(`/procedures/${p.id}`)}>
                                            Open in Builder
                                        </button>
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
