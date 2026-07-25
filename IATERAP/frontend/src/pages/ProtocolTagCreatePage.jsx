import { useNavigate } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import ProtocolTagForm from "../components/ProtocolTagForm";

export default function ProtocolTagCreatePage() {
    const navigate = useNavigate();

    return (
        <ManagementLayout
            title="Create Protocol Tag"
            description="Capture a new protocol tag and bind it to the LRU that owns it."
        >
            <ProtocolTagForm onSuccess={() => navigate("/protocol-tags")} />
        </ManagementLayout>
    );
}
