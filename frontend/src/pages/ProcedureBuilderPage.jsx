import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import ManagementLayout from "../components/ManagementLayout";
import TestStepCard from "../components/TestStepCard";
import { testProcedureService } from "../services/testProcedureService";
import { testStepService } from "../services/testStepService";

export default function ProcedureBuilderPage() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [procedure, setProcedure] = useState(null);
    const [steps, setSteps] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const loadAll = useCallback(async () => {
        try {
            setError("");
            const [procRes, stepsRes] = await Promise.all([
                testProcedureService.getProcedureById(id),
                testProcedureService.getSteps(id),
            ]);
            setProcedure(procRes.data);
            setSteps(stepsRes.data);
        } catch (fetchError) {
            console.error("Failed to load Procedure Builder data:", fetchError);
            setError("Unable to load this Test Procedure right now.");
        } finally {
            setLoading(false);
        }
    }, [id]);

    useEffect(() => {
        void loadAll();
    }, [loadAll]);

    const handleAddStep = async (stepPayload) => {
        await testProcedureService.addStep(id, stepPayload);
        await loadAll();
    };

    const handleUpdateStep = async (stepId, stepPayload) => {
        await testStepService.updateStep(stepId, stepPayload);
        await loadAll();
    };

    const handleDeleteStep = async (stepId) => {
        const isConfirmed = window.confirm("Delete this Test Step? This action cannot be undone.");
        if (!isConfirmed) {
            return;
        }

        await testStepService.deleteStep(stepId);
        await loadAll();
    };

    const handleMove = async (index, direction) => {
        const targetIndex = index + direction;
        if (targetIndex < 0 || targetIndex >= steps.length) {
            return;
        }

        const reordered = [...steps];
        const [moved] = reordered.splice(index, 1);
        reordered.splice(targetIndex, 0, moved);

        try {
            await testProcedureService.reorderSteps(id, reordered.map((s) => s.id));
            await loadAll();
        } catch (reorderError) {
            console.error("Failed to reorder Test Steps:", reorderError);
            setError("Could not reorder Test Steps. Please try again.");
        }
    };

    if (loading) {
        return (
            <ManagementLayout title="Procedure Builder" description="Loading...">
                <p className="helper-text">Loading Procedure Builder...</p>
            </ManagementLayout>
        );
    }

    if (error || !procedure) {
        return (
            <ManagementLayout title="Procedure Builder" description="">
                <p style={{ color: "var(--danger)" }}>{error || "Test Procedure not found."}</p>
                <button onClick={() => navigate("/procedures")}>Back to Procedures</button>
            </ManagementLayout>
        );
    }

    return (
        <ManagementLayout
            title={`Procedure Builder - ${procedure.name}`}
            description={`LRU: ${procedure.lru?.name || "-"}  |  Status: ${procedure.approvalStatus}  |  Version: ${procedure.versionNumber}`}
        >
            {error ? <p style={{ color: "var(--danger)" }}>{error}</p> : null}

            <section className="stack">
                {!steps.length ? (
                    <div className="empty-state">
                        No Test Steps yet. Add the first one below.
                    </div>
                ) : (
                    steps.map((step, index) => (
                        <TestStepCard
                            key={step.id}
                            step={step}
                            lruId={procedure.lru?.id}
                            onSave={(payload) => handleUpdateStep(step.id, payload)}
                            onDelete={handleDeleteStep}
                            onMoveUp={() => handleMove(index, -1)}
                            onMoveDown={() => handleMove(index, 1)}
                            isFirst={index === 0}
                            isLast={index === steps.length - 1}
                        />
                    ))
                )}

                <TestStepCard
                    step={null}
                    lruId={procedure.lru?.id}
                    onSave={handleAddStep}
                />
            </section>
        </ManagementLayout>
    );
}
