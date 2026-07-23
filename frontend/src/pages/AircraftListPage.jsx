import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import ManagementLayout from "../components/ManagementLayout";
import { aircraftService } from "../services/aircraftService";

export default function AircraftListPage() {
    const [aircraft, setAircraft] = useState([]);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchAircraft = async () => {
            try {
                const response = await aircraftService.getAllAircraft();
                setAircraft(response.data);
            } catch (error) {
                console.error("Failed to fetch aircraft:", error);
            }
        };

        fetchAircraft();
    }, []);

    return (
        <ManagementLayout
            eyebrow="Top hierarchy"
            title="Aircraft Management"
            description="Manage aircraft records that sit under the project layer."
        >
            <div className="page-stack">
                <div className="page-header">
                    <div>
                        <h2>Aircraft</h2>
                        <p>
                            Keep aircraft records ready for subsystem grouping and
                            downstream LRU work.
                        </p>
                    </div>

                    <div className="section-actions">
                        <button onClick={() => navigate("/aircraft/create")}>
                            Add Aircraft
                        </button>
                    </div>
                </div>

                {aircraft.length === 0 ? (
                    <div className="empty-state">
                        No aircraft records yet. Add the first aircraft to get
                        started.
                    </div>
                ) : (
                    <div className="card-grid">
                        {aircraft.map((item) => (
                            <article key={item.id} className="entity-card">
                                <h3>{item.name}</h3>
                                <p>{item.manufacturer}</p>
                            </article>
                        ))}
                    </div>
                )}
            </div>
        </ManagementLayout>
    );
}
