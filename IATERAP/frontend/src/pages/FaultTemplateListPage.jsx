import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import FaultTemplateTable from "../components/FaultTemplateTable";
import { faultTemplateService } from "../services/faultTemplateService";

export default function FaultTemplateListPage() {
    const navigate = useNavigate();
    const [faultTemplates, setFaultTemplates] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const loadFaultTemplates = async () => {
            try {
                setError("");
                const response =
                    await faultTemplateService.getAllFaultTemplates();
                setFaultTemplates(response.data);
            } catch (fetchError) {
                console.error("Failed to fetch fault templates:", fetchError);
                setError("Unable to load fault templates right now.");
            } finally {
                setLoading(false);
            }
        };

        void loadFaultTemplates();
    }, []);

    const handleDelete = async (id) => {
        const isConfirmed = window.confirm(
            "Delete this fault template? This action cannot be undone."
        );

        if (!isConfirmed) {
            return;
        }

        try {
            await faultTemplateService.deleteFaultTemplate(id);
            const response =
                await faultTemplateService.getAllFaultTemplates();
            setFaultTemplates(response.data);
        } catch (deleteError) {
            console.error("Failed to delete fault template:", deleteError);
            setError("The fault template could not be deleted.");
        }
    };

    return (
        <ManagementLayout
            title="Fault Template Management"
            description="Maintain fault templates per LRU so failure definitions stay traceable across the fleet."
        >
            <div className="page-toolbar">
                <p className="helper-text">
                    Fault templates capture the standard fault definitions
                    used to build FMEA branch mappings.
                </p>

                <button onClick={() => navigate("/fault-templates/create")}>
                    Create Fault Template
                </button>
            </div>

            <section className="page-card stack">
                {error ? <p style={{ color: "var(--danger)" }}>{error}</p> : null}

                {loading ? (
                    <p className="helper-text">Loading fault templates…</p>
                ) : (
                    <FaultTemplateTable
                        data={faultTemplates}
                        onView={(id) => navigate(`/fault-templates/${id}`)}
                        onEdit={(id) =>
                            navigate(`/fault-templates/${id}/edit`)
                        }
                        onDelete={handleDelete}
                    />
                )}
            </section>
        </ManagementLayout>
    );
}
