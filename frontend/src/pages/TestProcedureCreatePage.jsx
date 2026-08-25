import { useNavigate } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import TestProcedureForm from "../components/TestProcedureForm";

export default function TestProcedureCreatePage() {
    const navigate = useNavigate();

    const handleSuccess = (created) => {
        navigate(`/procedures/${created.id}/builder`);
    };

    return (
        <ManagementLayout
            title="Create Test Procedure"
            description="A new Test Procedure belongs to a single LRU; its Test Steps can only reference that LRU's own Interfaces, Connectors, Signals, Protocol Tags and Fault Templates."
        >
            <section className="page-card">
                <TestProcedureForm onSuccess={handleSuccess} />
            </section>
        </ManagementLayout>
    );
}
