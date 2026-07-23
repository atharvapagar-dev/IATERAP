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
            title="Subsystem Management"
            description="Manage subsystem definitions from the top hierarchy."
        >
            <div style={{ marginBottom: "16px" }}>
                <button onClick={() => navigate("/subsystems/create")}>
                    Add Subsystem
                </button>
            </div>

            <ul>
                {subsystems.map((item) => (
                    <li key={item.id}>{item.name}</li>
                ))}
            </ul>
        </ManagementLayout>
    );
}
