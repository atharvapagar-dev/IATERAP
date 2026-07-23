import { useState } from "react";
import { useNavigate } from "react-router-dom";
import ManagementLayout from "../components/ManagementLayout";
import { subsystemService } from "../services/subsystemService";

export default function SubsystemCreatePage() {
    const navigate = useNavigate();

    const [name, setName] = useState("");

    const handleSubmit = async (event) => {
        event.preventDefault();

        try {
            await subsystemService.createSubsystem({
                name,
            });

            navigate("/subsystems");
        } catch (error) {
            console.error(
                "Failed to create subsystem:",
                error
            );
        }
    };

    return (
        <ManagementLayout
            eyebrow="Top hierarchy"
            title="Create Subsystem"
            description="Add a new subsystem record."
        >
            <div className="page-stack">
                <div className="page-header">
                    <div>
                        <h2>Subsystem details</h2>
                        <p>
                            Add a subsystem so the hierarchy stays structured and
                            easy to navigate.
                        </p>
                    </div>
                </div>

                <form className="form-grid" onSubmit={handleSubmit}>
                    <div className="form-row">
                        <label htmlFor="subsystem-name">Subsystem name</label>
                        <input
                            id="subsystem-name"
                            type="text"
                            placeholder="Subsystem name"
                            value={name}
                            onChange={(event) =>
                                setName(event.target.value)
                            }
                        />
                    </div>

                    <div className="form-actions">
                        <button type="submit">Save Subsystem</button>
                        <button
                            type="button"
                            className="button button--ghost"
                            onClick={() => navigate("/subsystems")}
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            </div>
        </ManagementLayout>
    );
}
