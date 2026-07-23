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
            title="Create Project"
            description="Add the top-level project record that groups the rest of the aircraft engineering work."
        >
            <form
                onSubmit={handleSubmit}
                style={{
                    display: "grid",
                    gap: "12px",
                    maxWidth: "420px",
                }}
            >
                <input
                    type="text"
                    placeholder="Project name"
                    value={name}
                    onChange={(event) => setName(event.target.value)}
                />

                <textarea
                    placeholder="Project description"
                    value={description}
                    onChange={(event) => setDescription(event.target.value)}
                    rows={4}
                />

                <button type="submit">Save Project</button>
            </form>
        </ManagementLayout>
    );
}
