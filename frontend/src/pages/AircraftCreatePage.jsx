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
            title="Create Aircraft"
            description="Add a new aircraft to the management layer."
        >
            <form onSubmit={handleSubmit}>
                <input
                    type="text"
                    placeholder="Aircraft Name"
                    value={name}
                    onChange={(event) =>
                        setName(event.target.value)
                    }
                />

                <br />
                <br />

                <input
                    type="text"
                    placeholder="Manufacturer"
                    value={manufacturer}
                    onChange={(event) =>
                        setManufacturer(event.target.value)
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
