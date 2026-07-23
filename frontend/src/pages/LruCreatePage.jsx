import { useNavigate } from "react-router-dom";

import LruForm from "../components/LruForm";
import ManagementLayout from "../components/ManagementLayout";

export default function LruCreatePage() {
    const navigate = useNavigate();

    const handleSuccess = () => {
        navigate("/lrus");
    };

    return (
        <ManagementLayout
            eyebrow="Engineering data"
            title="Create LRU"
            description="Add a new line-replaceable unit and connect it to the right subsystem."
        >
            <div className="page-stack">
                <div className="page-header">
                    <div>
                        <h2>LRU details</h2>
                        <p>
                            Capture the key inventory details before linking the
                            unit to downstream workflows.
                        </p>
                    </div>
                </div>

                <LruForm onSuccess={handleSuccess} />
            </div>
        </ManagementLayout>
    );
}
