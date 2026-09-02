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
            <div className="page-toolbar">
                <div><p className="eyebrow">ACTIVE WORKSPACES</p><p className="helper-text">Select a programme to inspect its engineering context.</p></div>
                <button onClick={() => navigate("/projects/create")}>+ Create Project</button>
            </div>

            <div className="project-grid">
                {projects.map((project) => (
                    <section
                        key={project.id}
                        className="project-card"
                    >
                        <div className="project-card__top"><span className="status-pill">ACTIVE</span><span className="project-card__id">PRJ-{String(project.id).padStart(3, "0")}</span></div>
                        <h2>{project.name}</h2>
                        <p>{project.description || "No description provided."}</p>
                        <button className="project-card__link" onClick={() => navigate(`/projects/${project.id}`)}>Open workspace <span>↗</span></button>
                    </section>
                ))}
            </div>
        </ManagementLayout>
    );
}
