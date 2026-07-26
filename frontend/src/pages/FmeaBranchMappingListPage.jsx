import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import FmeaBranchMappingTable from "../components/FmeaBranchMappingTable";
import { fmeaBranchMappingService } from "../services/fmeaBranchMappingService";

export default function FmeaBranchMappingListPage() {
    const navigate = useNavigate();
    const [mappings, setMappings] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const loadMappings = async () => {
            try {
                setError("");
                const response =
                    await fmeaBranchMappingService.getAllFmeaBranchMappings();
                setMappings(response.data);
            } catch (fetchError) {
                console.error(
                    "Failed to fetch FMEA branch mappings:",
                    fetchError
                );
                setError("Unable to load FMEA branch mappings right now.");
            } finally {
                setLoading(false);
            }
        };

        void loadMappings();
    }, []);

    const handleDelete = async (id) => {
        const isConfirmed = window.confirm(
            "Delete this FMEA branch mapping? This action cannot be undone."
        );

        if (!isConfirmed) {
            return;
        }

        try {
            await fmeaBranchMappingService.deleteFmeaBranchMapping(id);
            const response =
                await fmeaBranchMappingService.getAllFmeaBranchMappings();
            setMappings(response.data);
        } catch (deleteError) {
            console.error(
                "Failed to delete FMEA branch mapping:",
                deleteError
            );
            setError("The FMEA branch mapping could not be deleted.");
        }
    };

    return (
        <ManagementLayout
            title="FMEA Branch Mapping Management"
            description="Map fault templates onto FMEA taxonomy branches per LRU, capturing effect, cause and risk priority."
        >
            <div className="page-toolbar">
                <p className="helper-text">
                    Each mapping links a fault template to its FMEA branch,
                    failure effect, and risk priority number.
                </p>

                <button
                    onClick={() => navigate("/fmea-branch-mappings/create")}
                >
                    Create FMEA Branch Mapping
                </button>
            </div>

            <section className="page-card stack">
                {error ? <p style={{ color: "var(--danger)" }}>{error}</p> : null}

                {loading ? (
                    <p className="helper-text">
                        Loading FMEA branch mappings…
                    </p>
                ) : (
                    <FmeaBranchMappingTable
                        data={mappings}
                        onView={(id) =>
                            navigate(`/fmea-branch-mappings/${id}`)
                        }
                        onEdit={(id) =>
                            navigate(`/fmea-branch-mappings/${id}/edit`)
                        }
                        onDelete={handleDelete}
                    />
                )}
            </section>
        </ManagementLayout>
    );
}
