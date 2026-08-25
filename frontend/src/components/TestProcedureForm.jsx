import { useEffect, useMemo, useState } from "react";

import { getLrus } from "../services/lruService";
import { testProcedureService } from "../services/testProcedureService";

export default function TestProcedureForm({ initialData, onSuccess }) {
    const initialFormState = useMemo(
        () => ({
            name: initialData?.name || "",
            description: initialData?.description || "",
            lruId: initialData?.lru?.id ? String(initialData.lru.id) : "",
        }),
        [initialData]
    );

    const [formState, setFormState] = useState(initialFormState);
    const [lrus, setLrus] = useState([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        const loadLrus = async () => {
            try {
                const response = await getLrus();
                setLrus(response.data);
            } catch (fetchError) {
                console.error("Failed to load LRUs:", fetchError);
                setError("Unable to load LRU options right now.");
            } finally {
                setLoading(false);
            }
        };

        loadLrus();
    }, []);

    const handleChange = (event) => {
        const { name, value } = event.target;
        setFormState((current) => ({ ...current, [name]: value }));
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError("");
        setSaving(true);

        const payload = {
            name: formState.name.trim(),
            description: formState.description.trim(),
            lru: { id: Number(formState.lruId) },
        };

        try {
            let result;
            if (initialData?.id) {
                result = await testProcedureService.updateProcedure(initialData.id, payload);
            } else {
                result = await testProcedureService.createProcedure(payload);
            }

            if (onSuccess) {
                onSuccess(result.data);
            }
        } catch (saveError) {
            console.error("Failed to save Test Procedure:", saveError);
            setError("Could not save the Test Procedure. Please review the form and try again.");
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return <p className="helper-text">Loading form options...</p>;
    }

    return (
        <form className="form-card" onSubmit={handleSubmit}>
            <div className="form-grid form-grid--two">
                <div className="form-field">
                    <label htmlFor="procedure-name">Name</label>
                    <input
                        id="procedure-name"
                        type="text"
                        name="name"
                        placeholder="Test procedure name"
                        value={formState.name}
                        onChange={handleChange}
                        required
                    />
                </div>

                <div className="form-field">
                    <label htmlFor="procedure-lru">LRU</label>
                    <select
                        id="procedure-lru"
                        name="lruId"
                        value={formState.lruId}
                        onChange={handleChange}
                        required
                    >
                        <option value="">Select LRU</option>
                        {lrus.map((item) => (
                            <option key={item.id} value={item.id}>{item.name}</option>
                        ))}
                    </select>
                </div>
            </div>

            <div className="form-field" style={{ marginTop: "16px" }}>
                <label htmlFor="procedure-description">Description</label>
                <textarea
                    id="procedure-description"
                    name="description"
                    placeholder="Optional details about this procedure"
                    value={formState.description}
                    onChange={handleChange}
                    rows={4}
                />
            </div>

            {error ? <p style={{ color: "var(--danger)", marginTop: "12px" }}>{error}</p> : null}

            <div className="form-actions">
                <button type="submit" disabled={saving}>
                    {saving ? "Saving..." : initialData ? "Update Procedure" : "Create Procedure"}
                </button>
            </div>
        </form>
    );
}
