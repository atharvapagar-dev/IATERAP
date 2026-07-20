import React, { useEffect, useState } from "react";
import { healthService } from "../services/healthService";
import HealthStatusCard from "../components/HealthStatusCard";

function HealthCheckPage() {
  const [status, setStatus] = useState("Checking...");
  const [message, setMessage] = useState("Connecting to backend...");

  useEffect(() => {
    const checkBackendHealth = async () => {
      try {
        await healthService.checkHealth();

        setStatus("Healthy");
        setMessage("Backend is running successfully.");
      } catch (error) {
        setStatus("Unavailable");
        setMessage("Unable to connect to the backend.");
      }
    };

    checkBackendHealth();
  }, []);

  return (
    <HealthStatusCard
      status={status}
      message={message}
    />
  );
}

export default HealthCheckPage;