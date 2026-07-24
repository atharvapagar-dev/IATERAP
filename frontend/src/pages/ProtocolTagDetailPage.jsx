import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import { protocolTagService } from "../services/protocolTagService";

export default function ProtocolTagDetailPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [protocolTag, setProtocolTag] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const loadProtocolTag = async () => {
            try {
                const response = await protocolTagService.getProtocolTagById(id);
                setProtocolTag(response.data);
            } catch (fetchError) {
                console.error("Failed to fetch protocol tag detail:", fetchError);
                setError("Unable to load the selected protocol tag.");
            } finally {
                setLoading(false);
            }
        };

        loadProtocolTag();
    }, [id]);

    return (
        <ManagementLayout
            title="Protocol Tag Details"
            description="Review the tag value, owning LRU, and supporting notes."
        >
            <div className="page-toolbar">
                <button
                    className="button--secondary"
                    onClick={() => navigate("/protocol-tags")}
                >
                    Back to list
                </button>

                <button onClick={() => navigate(`/protocol-tags/${id}/edit`)}>
                    Edit protocol tag
                </button>
            </div>

            {loading ? (
                <p className="helper-text">Loading protocol tag…</p>
            ) : error ? (
                <div className="empty-state">{error}</div>
            ) : protocolTag ? (
                <section className="page-card">
                    <div className="detail-grid">
                        <div className="detail-item">
                            <p className="detail-item__label">Name</p>
                            <p className="detail-item__value">
                                {protocolTag.name}
                            </p>
                        </div>

                        <div className="detail-item">
                            <p className="detail-item__label">Value</p>
                            <p className="detail-item__value">
                                {protocolTag.value}
                            </p>
                        </div>

                        <div className="detail-item">
                            <p className="detail-item__label">LRU</p>
                            <p className="detail-item__value">
                                {protocolTag.lru?.name || "—"}
                            </p>
                        </div>
                    </div>

                    <div style={{ marginTop: "18px" }}>
                        <p className="detail-item__label">Description</p>
                        <p className="helper-text">
                            {protocolTag.description || "No description provided."}
                        </p>
                    </div>
                </section>
            ) : null}
        </ManagementLayout>
    );
}
