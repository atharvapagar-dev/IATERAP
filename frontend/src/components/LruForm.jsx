import { useState } from "react";

import { createLru, updateLru } from "../services/lruService";

export default function LruForm({ initialData, onSuccess }) {
    const [name, setName] = useState(initialData?.name || "");
    const [subsystemName, setSubsystemName] = useState(
        initialData?.subsystemName || initialData?.subsystem?.name || ""
    );
    const [description, setDescription] = useState(
        initialData?.description || ""
    );
    const [isSaving, setIsSaving] = useState(false);

    const handleSubmit = async (event) => {
        event.preventDefault();
        setIsSaving(true);

        try {
            const payload = {
                name,
                subsystemName,
                description,
            };

            if (initialData) {
                await updateLru(initialData.id, payload);
            } else {
                await createLru(payload);
            }

            if (onSuccess) {
                onSuccess();
            }
        } catch (error) {
            console.error("Failed to save LRU:", error);
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <form className="form-grid" onSubmit={handleSubmit}>
            <div className="form-row">
                <label htmlFor="lru-name">LRU name</label>
                <input
                    id="lru-name"
                    type="text"
                    placeholder="Enter LRU name"
                    value={name}
                    onChange={(event) => setName(event.target.value)}
                />
            </div>

            <div className="form-row">
                <label htmlFor="lru-subsystem">Subsystem</label>
                <input
                    id="lru-subsystem"
                    type="text"
                    placeholder="Subsystem name"
                    value={subsystemName}
                    onChange={(event) => setSubsystemName(event.target.value)}
                />
                <span className="helper-text">
                    Use the subsystem name that owns this LRU.
                </span>
            </div>

            <div className="form-row">
                <label htmlFor="lru-description">Description</label>
                <textarea
                    id="lru-description"
                    placeholder="Optional LRU description"
                    rows={4}
                    value={description}
                    onChange={(event) => setDescription(event.target.value)}
                />
            </div>

            <div className="form-actions">
                <button type="submit" disabled={isSaving}>
                    {isSaving
                        ? "Saving..."
                        : initialData
                          ? "Update LRU"
                          : "Save LRU"}
                </button>
            </div>
        </form>
    );
}
