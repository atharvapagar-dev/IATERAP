import { useEffect, useState } from "react";

import ManagementLayout from "../components/ManagementLayout";
import { auditLogService } from "../services/auditLogService";

export default function AuditLogListPage() {
    const [auditLogs, setAuditLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const loadAuditLogs = async () => {
            try {
                setError("");
                const response = await auditLogService.getAllAuditLogs();
                setAuditLogs(response.data);
            } catch (fetchError) {
                console.error("Failed to fetch audit logs:", fetchError);
                setError("Unable to load audit logs right now.");
            } finally {
                setLoading(false);
            }
        };

        void loadAuditLogs();
    }, []);

    return (
        <ManagementLayout
            title="Audit Log"
            description="Every create, update, and delete performed through the platform is recorded here with actor, before state and after state."
        >
            <section className="page-card stack">
                {error ? <p style={{ color: "var(--danger)" }}>{error}</p> : null}

                {loading ? (
                    <p className="helper-text">Loading audit log…</p>
                ) : auditLogs.length ? (
                    <table className="entity-table">
                        <thead>
                            <tr>
                                <th>Timestamp</th>
                                <th>Actor</th>
                                <th>Action</th>
                                <th>Entity</th>
                                <th>Entity ID</th>
                                <th>Metadata</th>
                            </tr>
                        </thead>
                        <tbody>
                            {auditLogs.map((entry) => (
                                <tr key={entry.id}>
                                    <td>{entry.createdAt}</td>
                                    <td>{entry.actor}</td>
                                    <td>{entry.action}</td>
                                    <td>{entry.entityName}</td>
                                    <td>{entry.entityId || "—"}</td>
                                    <td>{entry.metadata || "—"}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                ) : (
                    <div className="empty-state">
                        No audit entries have been recorded yet.
                    </div>
                )}
            </section>
        </ManagementLayout>
    );
}
