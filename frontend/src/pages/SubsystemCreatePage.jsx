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
            title="Create Subsystem"
            description="Add a new subsystem record."
        >
            <form onSubmit={handleSubmit}>
                <input
                    type="text"
                    placeholder="Subsystem Name"
                    value={name}
                    onChange={(event) =>
                        setName(event.target.value)
                    }
                />

                <br />
                <br />

                <button type="submit">
                    Save
                </button>
            </form>
        </ManagementLayout>
    );
}
