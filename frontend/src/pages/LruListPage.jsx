import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import LruTable from "../components/LruTable";
import { deleteLru, getLrus } from "../services/lruService";

export default function LruListPage() {
    const [lrus, setLrus] = useState([]);
    const navigate = useNavigate();

    const loadLrus = () => {
        getLrus()
            .then((response) => {
                setLrus(response.data);
            })
            .catch((error) => {
                console.error("Failed to fetch LRUs:", error);
            });
    };

    useEffect(() => {
        loadLrus();
    }, []);

    const handleDelete = async (id) => {
        try {
            await deleteLru(id);
            loadLrus();
        } catch (error) {
            console.error("Failed to delete LRU:", error);
        }
    };

    return (
        <ManagementLayout
            eyebrow="Engineering data"
            title="LRU Management"
            description="Browse, inspect, and update line-replaceable units inside the hierarchy."
        >
            <div className="page-stack">
                <div className="stats-row">
                    <article className="metric-card">
                        <span className="eyebrow">Total LRUs</span>
                        <strong>{lrus.length}</strong>
                        <p>Loaded from the backend service.</p>
                    </article>
                </div>

                <div className="page-header">
                    <div>
                        <h2>LRUs</h2>
                        <p>
                            Manage the inventory that sits under each subsystem.
                        </p>
                    </div>

                    <div className="section-actions">
                        <button onClick={() => navigate("/lrus/create")}>
                            Add LRU
                        </button>
                    </div>
                </div>

                <LruTable
                    data={lrus}
                    onView={(id) => navigate("/lrus/" + id)}
                    onEdit={(id) => navigate("/lrus/" + id + "/edit")}
                    onDelete={handleDelete}
                />
            </div>
        </ManagementLayout>
    );
}
