import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import FmeaBranchMappingForm from "../components/FmeaBranchMappingForm";
import { fmeaBranchMappingService } from "../services/fmeaBranchMappingService";

export default function FmeaBranchMappingEditPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [mapping, setMapping] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const loadMapping = async () => {
            try {
                const response =
                    await fmeaBranchMappingService.getFmeaBranchMappingById(
                        id
                    );
                setMapping(response.data);
            } catch (fetchError) {
                console.error("Failed to fetch FMEA branch mapping:", fetchError);
                setError("Unable to load the selected FMEA branch mapping.");
            } finally {
                setLoading(false);
            }
        };

        loadMapping();
    }, [id]);

    return (
        <ManagementLayout
            title="Edit FMEA Branch Mapping"
            description="Update the branch code, effect, cause, detection method, action, or risk priority."
        >
            {loading ? (
                <p className="helper-text">Loading FMEA branch mapping…</p>
            ) : error ? (
                <div className="empty-state">{error}</div>
            ) : (
                <FmeaBranchMappingForm
                    initialData={mapping}
                    onSuccess={() => navigate("/fmea-branch-mappings")}
                />
            )}
        </ManagementLayout>
    );
}
