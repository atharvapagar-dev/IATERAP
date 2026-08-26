import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import { projectService } from "../services/projectService";

export default function ProjectDetailPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [project, setProject] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const loadProject = async () => {
            try {
                const response = await projectService.getProjectById(id);
                setProject(response.data);
            } catch (fetchError) {
                console.error("Failed to fetch project detail:", fetchError);
                setError("Unable to load the selected project.");
            } finally {
                setLoading(false);
            }
        };

        void loadProject();
    }, [id]);

    return (
        <ManagementLayout
            title="Project Details"
            description="Review the top-level project record for this engineering workspace."
        >
            <div className="page-toolbar">
                <button
                    className="button--secondary"
                    onClick={() => navigate("/projects")}
                >
                    Back to projects
                </button>
            </div>

            {loading ? (
                <p className="helper-text">Loading project...</p>
            ) : error ? (
                <div className="empty-state">{error}</div>
            ) : project ? (
                <section className="page-card">
                    <div className="detail-grid">
                        <div className="detail-item">
                            <p className="detail-item__label">Project ID</p>
                            <p className="detail-item__value">{project.id}</p>
                        </div>
                        <div className="detail-item">
                            <p className="detail-item__label">Name</p>
                            <p className="detail-item__value">{project.name}</p>
                        </div>
                    </div>

                    <div style={{ marginTop: "18px" }}>
                        <p className="detail-item__label">Description</p>
                        <p className="helper-text">
                            {project.description || "No description provided."}
                        </p>
                    </div>
                </section>
            ) : null}
        </ManagementLayout>
    );
}
