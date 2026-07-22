import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

import { getLruById } from "../services/lruService";

export default function LruDetailPage() {
    const { id } = useParams();

    const [lru, setLru] = useState(null);

    useEffect(() => {
        getLruById(id)
            .then((response) => setLru(response.data))
            .catch(console.error);
    }, [id]);

    if (!lru) {
        return <p>Loading LRU details...</p>;
    }

    return (
        <div>
            <h1>LRU Detail Page</h1>

            <p>ID: {lru.id}</p>
            <p>Name: {lru.name}</p>
        </div>
    );
}
