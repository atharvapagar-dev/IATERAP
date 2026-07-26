import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import { fmeaBranchMappingService } from "../services/fmeaBranchMappingService";

export default function FmeaBranchMappingDetailPage() {
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
                console.error(
                    "Failed to fetch FMEA branch mapping detail:",
                    fetchError
                );
                setError("Unable to load the selected FMEA branch mapping.");
            } finally {
                setLoading(false);
            }
        };

        loadMapping();
    }, [id]);

    return (
        <ManagementLayout
            title="FMEA Branch Mapping Details"
            description="Review the failure effect, cause, detection method, recommended action, and risk priority."
        >
            <div className="page-toolbar">
                <button
                    className="button--secondary"
                    onClick={() => navigate("/fmea-branch-mappings")}
                >
                    Back to list
                </button>

                <button
                    onClick={() =>
                        navigate(`/fmea-branch-mappings/${id}/edit`)
                    }
                >
                    Edit mapping
                </button>
            </div>

            {loading ? (
                <p className="helper-text">Loading FMEA branch mapping…</p>
            ) : error ? (
                <div className="empty-state">{error}</div>
            ) : mapping ? (
                <section className="page-card">
                    <div className="detail-grid">
                        <div className="detail-item">
                            <p className="detail-item__label">
                                FMEA Branch Code
                            </p>
                            <p className="detail-item__value">
                                {mapping.fmeaBranchCode}
                            </p>
                        </div>

                        <div className="detail-item">
                            <p className="detail-item__label">
                                Risk Priority Number
                            </p>
                            <p className="detail-item__value">
                                {mapping.riskPriorityNumber ?? "—"}
                            </p>
                        </div>

                        <div className="detail-item">
                            <p className="detail-item__label">
                                Fault Template
                            </p>
                            <p className="detail-item__value">
                                {mapping.faultTemplate
                                    ? `${mapping.faultTemplate.faultCode} — ${mapping.faultTemplate.name}`
                                    : "—"}
                            </p>
                        </div>

                        <div className="detail-item">
                            <p className="detail-item__label">LRU</p>
                            <p className="detail-item__value">
                                {mapping.lru?.name || "—"}
                            </p>
                        </div>
                    </div>

                    <div style={{ marginTop: "18px" }}>
                        <p className="detail-item__label">Failure Effect</p>
                        <p className="helper-text">
                            {mapping.failureEffect}
                        </p>
                    </div>

                    <div style={{ marginTop: "18px" }}>
                        <p className="detail-item__label">Failure Cause</p>
                        <p className="helper-text">
                            {mapping.failureCause || "Not documented."}
                        </p>
                    </div>

                    <div style={{ marginTop: "18px" }}>
                        <p className="detail-item__label">
                            Detection Method
                        </p>
                        <p className="helper-text">
                            {mapping.detectionMethod || "Not documented."}
                        </p>
                    </div>

                    <div style={{ marginTop: "18px" }}>
                        <p className="detail-item__label">
                            Recommended Action
                        </p>
                        <p className="helper-text">
                            {mapping.recommendedAction || "Not documented."}
                        </p>
                    </div>
                </section>
            ) : null}
        </ManagementLayout>
    );
}
