import { useEffect, useMemo, useState } from "react";

import { getLrus } from "../services/lruService";
import { faultTemplateService } from "../services/faultTemplateService";

const SEVERITY_OPTIONS = ["NORMAL", "WARNING", "CRITICAL", "CATASTROPHIC"];

export default function FaultTemplateForm({
    initialData,
    onSuccess,
}) {
    const initialFormState = useMemo(
        () => ({
            name: initialData?.name || "",
            faultCode: initialData?.faultCode || "",
            description: initialData?.description || "",
            severity: initialData?.severity || "NORMAL",
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
            faultCode: formState.faultCode.trim(),
            description: formState.description.trim(),
            severity: formState.severity,
            lru: {
                id: Number(formState.lruId),
            },
        };

        try {
            if (initialData?.id) {
                await faultTemplateService.updateFaultTemplate(
                    initialData.id,
                    payload
                );
            } else {
                await faultTemplateService.createFaultTemplate(payload);
            }

            if (onSuccess) {
                onSuccess();
            }
        } catch (saveError) {
            console.error("Failed to save fault template:", saveError);
            setError(
                "Could not save the fault template. Please review the form and try again."
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
                    <label htmlFor="fault-template-name">Name</label>
                    <input
                        id="fault-template-name"
                        type="text"
                        name="name"
                        placeholder="Fault template name"
                        value={formState.name}
                        onChange={handleChange}
                        required
                    />
                </div>

                <div className="form-field">
                    <label htmlFor="fault-template-code">Fault Code</label>
                    <input
                        id="fault-template-code"
                        type="text"
                        name="faultCode"
                        placeholder="e.g. FT-001"
                        value={formState.faultCode}
                        onChange={handleChange}
                        required
                    />
                </div>

                <div className="form-field">
                    <label htmlFor="fault-template-severity">Severity</label>
                    <select
                        id="fault-template-severity"
                        name="severity"
                        value={formState.severity}
                        onChange={handleChange}
                        required
                    >
                        {SEVERITY_OPTIONS.map((option) => (
                            <option key={option} value={option}>
                                {option}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="form-field">
                    <label htmlFor="fault-template-lru">LRU</label>
                    <select
                        id="fault-template-lru"
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
                <label htmlFor="fault-template-description">
                    Description
                </label>
                <textarea
                    id="fault-template-description"
                    name="description"
                    placeholder="Optional details about this fault"
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
                            ? "Update Fault Template"
                            : "Create Fault Template"}
                </button>
            </div>
        </form>
    );
}
