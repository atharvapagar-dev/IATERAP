import { useEffect, useMemo, useState } from "react";

import { getLrus } from "../services/lruService";
import { protocolTagService } from "../services/protocolTagService";

export default function ProtocolTagForm({
    initialData,
    onSuccess,
}) {
    const initialFormState = useMemo(
        () => ({
            name: initialData?.name || "",
            value: initialData?.value || "",
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
        setFormState((current) => ({
            ...current,
            [name]: value,
        }));
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError("");
        setSaving(true);

        const payload = {
            name: formState.name.trim(),
            value: formState.value.trim(),
            description: formState.description.trim(),
            lru: {
                id: Number(formState.lruId),
            },
        };

        try {
            if (initialData?.id) {
                await protocolTagService.updateProtocolTag(
                    initialData.id,
                    payload
                );
            } else {
                await protocolTagService.createProtocolTag(payload);
            }

            if (onSuccess) {
                onSuccess();
            }
        } catch (saveError) {
            console.error("Failed to save protocol tag:", saveError);
            setError(
                "Could not save the protocol tag. Please review the form and try again."
            );
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return <p className="helper-text">Loading form options…</p>;
    }

    return (
        <form className="form-card" onSubmit={handleSubmit}>
            <div className="form-grid form-grid--two">
                <div className="form-field">
                    <label htmlFor="protocol-tag-name">Name</label>
                    <input
                        id="protocol-tag-name"
                        type="text"
                        name="name"
                        placeholder="Protocol tag name"
                        value={formState.name}
                        onChange={handleChange}
                        required
                    />
                </div>

                <div className="form-field">
                    <label htmlFor="protocol-tag-value">Value</label>
                    <input
                        id="protocol-tag-value"
                        type="text"
                        name="value"
                        placeholder="Tag value"
                        value={formState.value}
                        onChange={handleChange}
                        required
                    />
                </div>

                <div className="form-field" style={{ gridColumn: "1 / -1" }}>
                    <label htmlFor="protocol-tag-lru">LRU</label>
                    <select
                        id="protocol-tag-lru"
                        name="lruId"
                        value={formState.lruId}
                        onChange={handleChange}
                        required
                    >
                        <option value="">Select LRU</option>
                        {lrus.map((item) => (
                            <option key={item.id} value={item.id}>
                                {item.name}
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            <div className="form-field" style={{ marginTop: "16px" }}>
                <label htmlFor="protocol-tag-description">Description</label>
                <textarea
                    id="protocol-tag-description"
                    name="description"
                    placeholder="Optional operational note"
                    value={formState.description}
                    onChange={handleChange}
                    rows={5}
                />
            </div>

            {error ? (
                <p style={{ color: "var(--danger)", marginTop: "12px" }}>
                    {error}
                </p>
            ) : null}

            <div className="form-actions">
                <button type="submit" disabled={saving}>
                    {saving
                        ? "Saving…"
                        : initialData
                            ? "Update Protocol Tag"
                            : "Create Protocol Tag"}
                </button>
            </div>
        </form>
    );
}
