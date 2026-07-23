import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import LruForm from "../components/LruForm";
import ManagementLayout from "../components/ManagementLayout";
import { getLruById } from "../services/lruService";

export default function LruEditPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [lru, setLru] = useState(null);

    useEffect(() => {
        getLruById(id)
            .then((response) => setLru(response.data))
            .catch((error) => console.error("Failed to load LRU:", error));
    }, [id]);

    const handleSuccess = () => {
        navigate("/lrus");
    };

    return (
        <ManagementLayout
            eyebrow="Engineering data"
            title="Edit LRU"
            description="Update the LRU details without losing the hierarchy context."
        >
            <div className="page-stack">
                <div className="page-header">
                    <div>
                        <h2>LRU record</h2>
                        <p>
                            Make the inventory changes and keep the subsystem
                            mapping aligned.
                        </p>
                    </div>
                </div>

                {lru ? (
                    <LruForm initialData={lru} onSuccess={handleSuccess} />
                ) : (
                    <div className="empty-state">Loading LRU details...</div>
                )}
            </div>
        </ManagementLayout>
    );
}
