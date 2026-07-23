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
            title="Aircraft Management"
            description="Manage aircraft records that sit under the project layer."
        >
            <div style={{ marginBottom: "16px" }}>
                <button onClick={() => navigate("/aircraft/create")}>
                    Add Aircraft
                </button>
            </div>

            <ul>
                {aircraft.map((item) => (
                    <li key={item.id}>
                        {item.name} - {item.manufacturer}
                    </li>
                ))}
            </ul>
        </ManagementLayout>
    );
}
