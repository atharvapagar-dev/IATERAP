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
            eyebrow="Top hierarchy"
            title="Project Management"
            description="Use projects as the top-level workspace, then drill into aircraft and subsystem administration from here."
        >
            <div className="page-stack">
                <div className="page-header">
                    <div>
                        <h2>Projects</h2>
                        <p>
                            Organize the program structure from the top down.
                        </p>
                    </div>

                    <div className="section-actions">
                        <button onClick={() => navigate("/projects/create")}>
                            Create Project
                        </button>
                    </div>
                </div>

                {projects.length === 0 ? (
                    <div className="empty-state">
                        No projects found yet. Create the first project to start
                        the hierarchy.
                    </div>
                ) : (
                    <div className="card-grid">
                        {projects.map((project) => (
                            <article key={project.id} className="entity-card">
                                <h3>{project.name}</h3>
                                <p>
                                    {project.description ||
                                        "No description provided."}
                                </p>
                            </article>
                        ))}
                    </div>
                )}
            </div>
        </ManagementLayout>
    );
}
