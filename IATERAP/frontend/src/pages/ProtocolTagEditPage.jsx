import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import ProtocolTagForm from "../components/ProtocolTagForm";
import { protocolTagService } from "../services/protocolTagService";

export default function ProtocolTagEditPage() {
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
                console.error("Failed to fetch protocol tag:", fetchError);
                setError("Unable to load the selected protocol tag.");
            } finally {
                setLoading(false);
            }
        };

        loadProtocolTag();
    }, [id]);

    return (
        <ManagementLayout
            title="Edit Protocol Tag"
            description="Update the tag name, value, description, or owning LRU."
        >
            {loading ? (
                <p className="helper-text">Loading protocol tag…</p>
            ) : error ? (
                <div className="empty-state">{error}</div>
            ) : (
                <ProtocolTagForm
                    initialData={protocolTag}
                    onSuccess={() => navigate("/protocol-tags")}
                />
            )}
        </ManagementLayout>
    );
}
