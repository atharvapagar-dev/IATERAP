import { useNavigate } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import SignalLibraryForm from "../components/SignalLibraryForm";

export default function SignalLibraryCreatePage() {
    const navigate = useNavigate();

    return (
        <ManagementLayout
            title="Create Signal Library"
            description="Register a new signal library and bind it to the correct interface and LRU."
        >
            <SignalLibraryForm onSuccess={() => navigate("/signal-libraries")} />
        </ManagementLayout>
    );
}
