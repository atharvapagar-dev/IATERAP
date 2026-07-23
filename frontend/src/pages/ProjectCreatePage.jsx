import { useState } from "react";
import { useNavigate } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import { projectService } from "../services/projectService";

export default function ProjectCreatePage() {
    const navigate = useNavigate();
    const [name, setName] = useState("");
    const [description, setDescription] = useState("");

    const handleSubmit = async (event) => {
        event.preventDefault();

        try {
            await projectService.createProject({
                name,
                description,
            });

            navigate("/projects");
        } catch (error) {
            console.error("Failed to create project:", error);
        }
    };

    return (
        <ManagementLayout
            eyebrow="Top hierarchy"
            title="Create Project"
            description="Add the top-level project record that groups the rest of the aircraft engineering work."
        >
            <div className="page-stack">
                <div className="page-header">
                    <div>
                        <h2>Project details</h2>
                        <p>
                            Capture the top-level program information before
                            adding aircraft and subsystem records.
                        </p>
                    </div>
                </div>

                <form className="form-grid" onSubmit={handleSubmit}>
                    <div className="form-row">
                        <label htmlFor="project-name">Project name</label>
                        <input
                            id="project-name"
                            type="text"
                            placeholder="Project name"
                            value={name}
                            onChange={(event) => setName(event.target.value)}
                        />
                    </div>

                    <div className="form-row">
                        <label htmlFor="project-description">Description</label>
                        <textarea
                            id="project-description"
                            placeholder="Project description"
                            value={description}
                            onChange={(event) =>
                                setDescription(event.target.value)
                            }
                            rows={4}
                        />
                    </div>

                    <div className="form-actions">
                        <button type="submit">Save Project</button>
                        <button
                            type="button"
                            className="button button--ghost"
                            onClick={() => navigate("/projects")}
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            </div>
        </ManagementLayout>
    );
}
