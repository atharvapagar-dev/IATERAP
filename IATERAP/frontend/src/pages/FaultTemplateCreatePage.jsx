import { useNavigate } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import FaultTemplateForm from "../components/FaultTemplateForm";

export default function FaultTemplateCreatePage() {
    const navigate = useNavigate();

    return (
        <ManagementLayout
            title="Create Fault Template"
            description="Define a new fault template and bind it to the LRU that owns it."
        >
            <FaultTemplateForm onSuccess={() => navigate("/fault-templates")} />
        </ManagementLayout>
    );
}
