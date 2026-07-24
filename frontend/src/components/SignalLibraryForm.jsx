import { useEffect, useMemo, useState } from "react";

import { interfaceService } from "../services/interfaceService";
import { getLrus } from "../services/lruService";
import { signalLibraryService } from "../services/signalLibraryService";

export default function SignalLibraryForm({
    initialData,
    onSuccess,
}) {
    const initialFormState = useMemo(
        () => ({
            name: initialData?.name || "",
            description: initialData?.description || "",
            engineeringUnit: initialData?.engineeringUnit || "",
            interfaceId: initialData?.interfaceEntity?.id
                ? String(initialData.interfaceEntity.id)
                : "",
            lruId: initialData?.lru?.id ? String(initialData.lru.id) : "",
        }),
        [initialData]
    );

    const [formState, setFormState] = useState(initialFormState);
    const [interfaces, setInterfaces] = useState([]);
    const [lrus, setLrus] = useState([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        const loadOptions = async () => {
            try {
                const [interfacesResponse, lrusResponse] =
                    await Promise.all([
                        interfaceService.getAllInterfaces(),
                        getLrus(),
                    ]);

                setInterfaces(interfacesResponse.data);
                setLrus(lrusResponse.data);
            } catch (fetchError) {
                console.error("Failed to load form options:", fetchError);
                setError(
                    "Unable to load interface and LRU options right now."
                );
            } finally {
                setLoading(false);
            }
        };

        loadOptions();
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
            description: formState.description.trim(),
            engineeringUnit: formState.engineeringUnit.trim(),
            interfaceEntity: {
                id: Number(formState.interfaceId),
            },
            lru: {
                id: Number(formState.lruId),
            },
        };

        try {
            if (initialData?.id) {
                await signalLibraryService.updateSignalLibrary(
                    initialData.id,
                    payload
                );
            } else {
                await signalLibraryService.createSignalLibrary(payload);
            }

            if (onSuccess) {
                onSuccess();
            }
        } catch (saveError) {
            console.error("Failed to save signal library:", saveError);
            setError(
                "Could not save the signal library. Please review the form and try again."
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
                    <label htmlFor="signal-library-name">Name</label>
                    <input
                        id="signal-library-name"
                        type="text"
                        name="name"
                        placeholder="Signal library name"
                        value={formState.name}
                        onChange={handleChange}
                        required
                    />
                </div>

                <div className="form-field">
                    <label htmlFor="signal-library-unit">
                        Engineering unit
                    </label>
                    <input
                        id="signal-library-unit"
                        type="text"
                        name="engineeringUnit"
                        placeholder="e.g. V, A, °C"
                        value={formState.engineeringUnit}
                        onChange={handleChange}
                    />
                </div>

                <div className="form-field">
                    <label htmlFor="signal-library-interface">
                        Interface
                    </label>
                    <select
                        id="signal-library-interface"
                        name="interfaceId"
                        value={formState.interfaceId}
                        onChange={handleChange}
                        required
                    >
                        <option value="">Select interface</option>
                        {interfaces.map((item) => (
                            <option key={item.id} value={item.id}>
                                {item.name}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="form-field">
                    <label htmlFor="signal-library-lru">LRU</label>
                    <select
                        id="signal-library-lru"
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
                <label htmlFor="signal-library-description">
                    Description
                </label>
                <textarea
                    id="signal-library-description"
                    name="description"
                    placeholder="What does this signal library represent?"
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
                            ? "Update Signal Library"
                            : "Create Signal Library"}
                </button>
            </div>
        </form>
    );
}
