import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import { getLruById } from "../services/lruService";

export default function LruDetailPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [lru, setLru] = useState(null);

    useEffect(() => {
        getLruById(id)
            .then((response) => setLru(response.data))
            .catch((error) => console.error("Failed to load LRU:", error));
    }, [id]);

    return (
        <ManagementLayout
            eyebrow="Engineering data"
            title="LRU Detail"
            description="Review the line-replaceable unit record and its hierarchy metadata."
        >
            {lru ? (
                <div className="page-stack">
                    <div className="page-header">
                        <div>
                            <h2>{lru.name || "Unnamed LRU"}</h2>
                            <p>
                                Inspect the inventory details and return to the
                                list when you’re done.
                            </p>
                        </div>

                        <div className="section-actions">
                            <button onClick={() => navigate("/lrus")}>
                                Back to list
                            </button>
                        </div>
                    </div>

                    <div className="card-grid">
                        <article className="entity-card">
                            <span className="eyebrow">Identity</span>
                            <h3>ID {lru.id}</h3>
                            <p>{lru.description || "No description provided."}</p>
                        </article>

                        <article className="entity-card">
                            <span className="eyebrow">Hierarchy</span>
                            <h3>{lru.subsystemName || lru.subsystem?.name || "Unassigned"}</h3>
                            <p>
                                {lru.partNumber
                                    ? `Part number: ${lru.partNumber}`
                                    : "No part number supplied."}
                            </p>
                        </article>

                        <article className="entity-card">
                            <span className="eyebrow">Reference</span>
                            <h3>
                                {lru.serialNumber || "Serial number unavailable"}
                            </h3>
                            <p>
                                {lru.manufacturer || "Manufacturer not captured yet."}
                            </p>
                        </article>
                    </div>
                </div>
            ) : (
                <div className="empty-state">Loading LRU details...</div>
            )}
        </ManagementLayout>
    );
}
