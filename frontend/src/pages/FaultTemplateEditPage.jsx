import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import FaultTemplateForm from "../components/FaultTemplateForm";
import { faultTemplateService } from "../services/faultTemplateService";

export default function FaultTemplateEditPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [faultTemplate, setFaultTemplate] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const loadFaultTemplate = async () => {
            try {
                const response =
                    await faultTemplateService.getFaultTemplateById(id);
                setFaultTemplate(response.data);
            } catch (fetchError) {
                console.error("Failed to fetch fault template:", fetchError);
                setError("Unable to load the selected fault template.");
            } finally {
                setLoading(false);
            }
        };

        loadFaultTemplate();
    }, [id]);

    return (
        <ManagementLayout
            title="Edit Fault Template"
            description="Update the fault definition, severity, or owning LRU."
        >
            {loading ? (
                <p className="helper-text">Loading fault template…</p>
            ) : error ? (
                <div className="empty-state">{error}</div>
            ) : (
                <FaultTemplateForm
                    initialData={faultTemplate}
                    onSuccess={() => navigate("/fault-templates")}
                />
            )}
        </ManagementLayout>
    );
}
