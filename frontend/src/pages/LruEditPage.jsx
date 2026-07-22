import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import { getLruById } from "../services/lruService";
import LruForm from "../components/LruForm";

export default function LruEditPage() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [lru, setLru] = useState(null);

    useEffect(() => {
        getLruById(id)
            .then((response) => setLru(response.data))
            .catch(console.error);
    }, [id]);

    const handleSuccess = () => {
        navigate("/lrus");
    };

    if (!lru) {
        return <p>Loading LRU...</p>;
    }

    return (
        <div>
            <h1>Edit LRU</h1>

            <LruForm
                initialData={lru}
                onSuccess={handleSuccess}
            />
        </div>
    );
}
