import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { deleteLru, getLrus } from "../services/lruService";
import LruTable from "../components/LruTable";

export default function LruListPage() {
    const [lrus, setLrus] = useState([]);
    const navigate = useNavigate();

    const loadLrus = () => {
        getLrus()
            .then((response) => {
                setLrus(response.data);
            })
            .catch(console.error);
    };

    useEffect(() => {
        loadLrus();
    }, []);

    const handleDelete = async (id) => {
        try {
            await deleteLru(id);
            loadLrus();
        } catch (error) {
            console.error("Failed to delete LRU:", error);
        }
    };

    return (
        <div>
            <h1>LRU List</h1>

            <LruTable
                data={lrus}
                onView={(id) => navigate("/lrus/" + id)}
                onEdit={(id) => navigate("/lrus/" + id + "/edit")}
                onDelete={handleDelete}
            />
        </div>
    );
}
