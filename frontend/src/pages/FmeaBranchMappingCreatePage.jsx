import { useNavigate } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import FmeaBranchMappingForm from "../components/FmeaBranchMappingForm";

export default function FmeaBranchMappingCreatePage() {
    const navigate = useNavigate();

    return (
        <ManagementLayout
            title="Create FMEA Branch Mapping"
            description="Link a fault template to its FMEA taxonomy branch, effect, cause and risk priority."
        >
            <FmeaBranchMappingForm
                onSuccess={() => navigate("/fmea-branch-mappings")}
            />
        </ManagementLayout>
    );
}
