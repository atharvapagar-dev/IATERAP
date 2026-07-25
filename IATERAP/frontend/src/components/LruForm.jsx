import { useState } from "react";
import {
    createLru,
    updateLru,
} from "../services/lruService";

export default function LruForm({
    initialData,
    onSuccess,
}) {
    const [name, setName] = useState(
        initialData?.name || ""
    );

    const handleSubmit = async (event) => {
        event.preventDefault();

        try {
            if (initialData) {
                await updateLru(initialData.id, { name });
            } else {
                await createLru({ name });
            }

            if (onSuccess) {
                onSuccess();
            }
        } catch (error) {
            console.error("Failed to save LRU:", error);
        }
    };

    return (
        <form onSubmit={handleSubmit}>
            <input
                type="text"
                placeholder="Enter LRU Name"
                value={name}
                onChange={(event) => setName(event.target.value)}
            />

            <button type="submit">
                {initialData ? "Update" : "Save"}
            </button>
        </form>
    );
}
