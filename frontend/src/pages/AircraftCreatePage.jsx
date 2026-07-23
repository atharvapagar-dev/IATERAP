import { useState } from "react";
import { useNavigate } from "react-router-dom";
import ManagementLayout from "../components/ManagementLayout";
import { aircraftService } from "../services/aircraftService";

export default function AircraftCreatePage() {
    const navigate = useNavigate();

    const [name, setName] = useState("");
    const [manufacturer, setManufacturer] = useState("");

    const handleSubmit = async (event) => {
        event.preventDefault();

        try {
            await aircraftService.createAircraft({
                name,
                manufacturer,
            });

            navigate("/aircraft");
        } catch (error) {
            console.error("Failed to create aircraft:", error);
        }
    };

    return (
        <ManagementLayout
            eyebrow="Top hierarchy"
            title="Create Aircraft"
            description="Add a new aircraft to the management layer."
        >
            <div className="page-stack">
                <div className="page-header">
                    <div>
                        <h2>Aircraft details</h2>
                        <p>
                            Enter the aircraft name and manufacturer to keep the
                            hierarchy organized.
                        </p>
                    </div>
                </div>

                <form className="form-grid" onSubmit={handleSubmit}>
                    <div className="form-row">
                        <label htmlFor="aircraft-name">Aircraft name</label>
                        <input
                            id="aircraft-name"
                            type="text"
                            placeholder="Aircraft name"
                            value={name}
                            onChange={(event) =>
                                setName(event.target.value)
                            }
                        />
                    </div>

                    <div className="form-row">
                        <label htmlFor="manufacturer">Manufacturer</label>
                        <input
                            id="manufacturer"
                            type="text"
                            placeholder="Manufacturer"
                            value={manufacturer}
                            onChange={(event) =>
                                setManufacturer(event.target.value)
                            }
                        />
                    </div>

                    <div className="form-actions">
                        <button type="submit">Save Aircraft</button>
                        <button
                            type="button"
                            className="button button--ghost"
                            onClick={() => navigate("/aircraft")}
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            </div>
        </ManagementLayout>
    );
}
