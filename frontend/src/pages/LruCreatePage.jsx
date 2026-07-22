import { useNavigate } from "react-router-dom";
import LruForm from "../components/LruForm";

export default function LruCreatePage() {
    const navigate = useNavigate();

    const handleSuccess = () => {
        navigate("/lrus");
    };

    return (
        <div>
            <h1>Create LRU</h1>

            <LruForm onSuccess={handleSuccess} />
        </div>
    );
}
