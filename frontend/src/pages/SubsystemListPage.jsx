import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import ManagementLayout from "../components/ManagementLayout";
import { subsystemService } from "../services/subsystemService";

export default function SubsystemListPage() {
    const [subsystems, setSubsystems] = useState([]);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchSubsystems = async () => {
            try {
                const response =
                    await subsystemService.getAllSubsystems();

                setSubsystems(response.data);
            } catch (error) {
                console.error(
                    "Failed to fetch subsystems:",
                    error
                );
            }
        };

        fetchSubsystems();
    }, []);

    return (
        <ManagementLayout
            eyebrow="Top hierarchy"
            title="Subsystem Management"
            description="Manage subsystem definitions from the top hierarchy."
        >
            <div className="page-stack">
                <div className="page-header">
                    <div>
                        <h2>Subsystems</h2>
                        <p>
                            Define subsystem groups before moving into the LRU
                            layer.
                        </p>
                    </div>

                    <div className="section-actions">
                        <button onClick={() => navigate("/subsystems/create")}>
                            Add Subsystem
                        </button>
                    </div>
                </div>

                {subsystems.length === 0 ? (
                    <div className="empty-state">
                        No subsystems created yet. Add one to begin structuring the
                        project.
                    </div>
                ) : (
                    <div className="card-grid">
                        {subsystems.map((item) => (
                            <article key={item.id} className="entity-card">
                                <h3>{item.name}</h3>
                            </article>
                        ))}
                    </div>
                )}
            </div>
        </ManagementLayout>
    );
}
