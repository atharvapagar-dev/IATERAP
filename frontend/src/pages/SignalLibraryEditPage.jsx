import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import SignalLibraryForm from "../components/SignalLibraryForm";
import { signalLibraryService } from "../services/signalLibraryService";

export default function SignalLibraryEditPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [signalLibrary, setSignalLibrary] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const loadSignalLibrary = async () => {
            try {
                const response =
                    await signalLibraryService.getSignalLibraryById(id);
                setSignalLibrary(response.data);
            } catch (fetchError) {
                console.error("Failed to fetch signal library:", fetchError);
                setError("Unable to load the selected signal library.");
            } finally {
                setLoading(false);
            }
        };

        loadSignalLibrary();
    }, [id]);

    return (
        <ManagementLayout
            title="Edit Signal Library"
            description="Update the signal library metadata or reassign its interface and LRU mapping."
        >
            {loading ? (
                <p className="helper-text">Loading signal library…</p>
            ) : error ? (
                <div className="empty-state">{error}</div>
            ) : (
                <SignalLibraryForm
                    initialData={signalLibrary}
                    onSuccess={() => navigate("/signal-libraries")}
                />
            )}
        </ManagementLayout>
    );
}
