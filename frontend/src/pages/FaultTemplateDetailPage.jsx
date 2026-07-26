import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import { faultTemplateService } from "../services/faultTemplateService";
import { fmeaBranchMappingService } from "../services/fmeaBranchMappingService";

export default function FaultTemplateDetailPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [faultTemplate, setFaultTemplate] = useState(null);
    const [mappings, setMappings] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const loadFaultTemplate = async () => {
            try {
                const [detailResponse, mappingsResponse] = await Promise.all([
                    faultTemplateService.getFaultTemplateById(id),
                    fmeaBranchMappingService.getFmeaBranchMappingsByFaultTemplateId(
                        id
                    ),
                ]);

                setFaultTemplate(detailResponse.data);
                setMappings(mappingsResponse.data);
            } catch (fetchError) {
                console.error(
                    "Failed to fetch fault template detail:",
                    fetchError
                );
                setError("Unable to load the selected fault template.");
            } finally {
                setLoading(false);
            }
        };

        loadFaultTemplate();
    }, [id]);

    return (
        <ManagementLayout
            title="Fault Template Details"
            description="Review the fault definition, owning LRU, and any FMEA branch mappings built from it."
        >
            <div className="page-toolbar">
                <button
                    className="button--secondary"
                    onClick={() => navigate("/fault-templates")}
                >
                    Back to list
                </button>

                <button onClick={() => navigate(`/fault-templates/${id}/edit`)}>
                    Edit fault template
                </button>
            </div>

            {loading ? (
                <p className="helper-text">Loading fault template…</p>
            ) : error ? (
                <div className="empty-state">{error}</div>
            ) : faultTemplate ? (
                <>
                    <section className="page-card">
                        <div className="detail-grid">
                            <div className="detail-item">
                                <p className="detail-item__label">Name</p>
                                <p className="detail-item__value">
                                    {faultTemplate.name}
                                </p>
                            </div>

                            <div className="detail-item">
                                <p className="detail-item__label">
                                    Fault Code
                                </p>
                                <p className="detail-item__value">
                                    {faultTemplate.faultCode}
                                </p>
                            </div>

                            <div className="detail-item">
                                <p className="detail-item__label">
                                    Severity
                                </p>
                                <p className="detail-item__value">
                                    {faultTemplate.severity}
                                </p>
                            </div>

                            <div className="detail-item">
                                <p className="detail-item__label">LRU</p>
                                <p className="detail-item__value">
                                    {faultTemplate.lru?.name || "—"}
                                </p>
                            </div>
                        </div>

                        <div style={{ marginTop: "18px" }}>
                            <p className="detail-item__label">Description</p>
                            <p className="helper-text">
                                {faultTemplate.description ||
                                    "No description provided."}
                            </p>
                        </div>
                    </section>

                    <section
                        className="page-card"
                        style={{ marginTop: "18px" }}
                    >
                        <p className="detail-item__label">
                            FMEA Branch Mappings using this fault template
                        </p>

                        {mappings.length ? (
                            <ul>
                                {mappings.map((mapping) => (
                                    <li key={mapping.id}>
                                        <a
                                            href={`/fmea-branch-mappings/${mapping.id}`}
                                            onClick={(event) => {
                                                event.preventDefault();
                                                navigate(
                                                    `/fmea-branch-mappings/${mapping.id}`
                                                );
                                            }}
                                        >
                                            {mapping.fmeaBranchCode} —{" "}
                                            {mapping.failureEffect}
                                        </a>
                                    </li>
                                ))}
                            </ul>
                        ) : (
                            <p className="helper-text">
                                No FMEA branch mappings reference this fault
                                template yet.
                            </p>
                        )}
                    </section>
                </>
            ) : null}
        </ManagementLayout>
    );
}
