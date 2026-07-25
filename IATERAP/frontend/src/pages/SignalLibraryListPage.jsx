import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import SignalLibraryTable from "../components/SignalLibraryTable";
import { signalLibraryService } from "../services/signalLibraryService";

export default function SignalLibraryListPage() {
    const navigate = useNavigate();
    const [signalLibraries, setSignalLibraries] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const loadSignalLibraries = async () => {
            try {
                setError("");
                const response =
                    await signalLibraryService.getAllSignalLibraries();
                setSignalLibraries(response.data);
            } catch (fetchError) {
                console.error("Failed to fetch signal libraries:", fetchError);
                setError("Unable to load signal libraries right now.");
            } finally {
                setLoading(false);
            }
        };

        void loadSignalLibraries();
    }, []);

    const handleDelete = async (id) => {
        const isConfirmed = window.confirm(
            "Delete this signal library? This action cannot be undone."
        );

        if (!isConfirmed) {
            return;
        }

        try {
            await signalLibraryService.deleteSignalLibrary(id);
            const response =
                await signalLibraryService.getAllSignalLibraries();
            setSignalLibraries(response.data);
        } catch (deleteError) {
            console.error("Failed to delete signal library:", deleteError);
            setError("The signal library could not be deleted.");
        }
    };

    return (
        <ManagementLayout
            title="Signal Library Management"
            description="Create and maintain signal libraries, mapped to the correct interface and LRU so downstream engineering data stays traceable."
        >
            <div className="page-toolbar">
                <p className="helper-text">
                    Manage the backbone catalogue used by the engineering UI.
                </p>

                <button onClick={() => navigate("/signal-libraries/create")}>
                    Create Signal Library
                </button>
            </div>

            <section className="page-card stack">
                {error ? <p style={{ color: "var(--danger)" }}>{error}</p> : null}

                {loading ? (
                    <p className="helper-text">Loading signal libraries…</p>
                ) : (
                    <SignalLibraryTable
                        data={signalLibraries}
                        onView={(id) => navigate(`/signal-libraries/${id}`)}
                        onEdit={(id) =>
                            navigate(`/signal-libraries/${id}/edit`)
                        }
                        onDelete={handleDelete}
                    />
                )}
            </section>
        </ManagementLayout>
    );
}
