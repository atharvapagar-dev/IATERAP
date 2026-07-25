import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import ProtocolTagTable from "../components/ProtocolTagTable";
import { protocolTagService } from "../services/protocolTagService";

export default function ProtocolTagListPage() {
    const navigate = useNavigate();
    const [protocolTags, setProtocolTags] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const loadProtocolTags = async () => {
            try {
                setError("");
                const response = await protocolTagService.getAllProtocolTags();
                setProtocolTags(response.data);
            } catch (fetchError) {
                console.error("Failed to fetch protocol tags:", fetchError);
                setError("Unable to load protocol tags right now.");
            } finally {
                setLoading(false);
            }
        };

        void loadProtocolTags();
    }, []);

    const handleDelete = async (id) => {
        const isConfirmed = window.confirm(
            "Delete this protocol tag? This action cannot be undone."
        );

        if (!isConfirmed) {
            return;
        }

        try {
            await protocolTagService.deleteProtocolTag(id);
            const response = await protocolTagService.getAllProtocolTags();
            setProtocolTags(response.data);
        } catch (deleteError) {
            console.error("Failed to delete protocol tag:", deleteError);
            setError("The protocol tag could not be deleted.");
        }
    };

    return (
        <ManagementLayout
            title="Protocol Tag Management"
            description="Keep protocol tags aligned to the correct LRU so the engineering hierarchy remains traceable."
        >
            <div className="page-toolbar">
                <p className="helper-text">
                    Use protocol tags to capture the LRU-specific command and
                    mapping metadata.
                </p>

                <button onClick={() => navigate("/protocol-tags/create")}>
                    Create Protocol Tag
                </button>
            </div>

            <section className="page-card stack">
                {error ? <p style={{ color: "var(--danger)" }}>{error}</p> : null}

                {loading ? (
                    <p className="helper-text">Loading protocol tags…</p>
                ) : (
                    <ProtocolTagTable
                        data={protocolTags}
                        onView={(id) => navigate(`/protocol-tags/${id}`)}
                        onEdit={(id) => navigate(`/protocol-tags/${id}/edit`)}
                        onDelete={handleDelete}
                    />
                )}
            </section>
        </ManagementLayout>
    );
}
