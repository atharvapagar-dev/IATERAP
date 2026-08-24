import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import { projectService } from "../services/projectService";

export default function ProjectListPage() {
    const navigate = useNavigate();
    const [projects, setProjects] = useState([]);

    useEffect(() => {
        const loadProjects = async () => {
            try {
                const response = await projectService.getAllProjects();
                setProjects(response.data);
            } catch (error) {
                console.error("Failed to fetch projects:", error);
            }
        };

        loadProjects();
    }, []);

    return (
        <ManagementLayout
            title="Project Management"
            description="Use projects as the top-level workspace, then drill into aircraft and subsystem administration from here."
        >
            <div style={{ marginBottom: "16px" }}>
                <button onClick={() => navigate("/projects/create")}>
                    Create Project
                </button>
            </div>

            <div
                style={{
                    display: "grid",
                    gap: "16px",
                    gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))",
                }}
            >
                {projects.map((project) => (
                    <section
                        key={project.id}
                        style={{
                            border: "1px solid #ddd",
                            borderRadius: "8px",
                            padding: "16px",
                        }}
                    >
                        <h2 style={{ marginTop: 0 }}>{project.name}</h2>
                        <p>{project.description || "No description provided."}</p>
                        <button onClick={() => navigate(`/projects/${project.id}`)}>
                            Open Project
                        </button>
                    </section>
                ))}
            </div>
        </ManagementLayout>
    );
}
