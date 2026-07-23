import { useEffect, useState } from "react";

import HealthStatusCard from "../components/HealthStatusCard";
import { healthService } from "../services/healthService";

function HealthCheckPage() {
    const [status, setStatus] = useState("Checking...");
    const [message, setMessage] = useState("Connecting to backend...");

    useEffect(() => {
        const checkBackendHealth = async () => {
            try {
                await healthService.checkHealth();
                setStatus("Healthy");
                setMessage("Backend is running successfully.");
            } catch {
                setStatus("Unavailable");
                setMessage("Unable to connect to the backend.");
            }
        };

        checkBackendHealth();
    }, []);

    return (
        <div className="app-shell">
            <div className="app-shell__frame">
                <HealthStatusCard
                    status={status}
                    message={message}
                />
            </div>
        </div>
    );
}

export default HealthCheckPage;
