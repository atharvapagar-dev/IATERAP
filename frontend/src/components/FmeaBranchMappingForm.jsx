import { useEffect, useMemo, useState } from "react";

import { getLrus } from "../services/lruService";
import { faultTemplateService } from "../services/faultTemplateService";
import { fmeaBranchMappingService } from "../services/fmeaBranchMappingService";

export default function FmeaBranchMappingForm({
    initialData,
    onSuccess,
}) {
    const initialFormState = useMemo(
        () => ({
            fmeaBranchCode: initialData?.fmeaBranchCode || "",
            failureEffect: initialData?.failureEffect || "",
            failureCause: initialData?.failureCause || "",
            detectionMethod: initialData?.detectionMethod || "",
            recommendedAction: initialData?.recommendedAction || "",
            riskPriorityNumber:
                initialData?.riskPriorityNumber !== undefined &&
                initialData?.riskPriorityNumber !== null
                    ? String(initialData.riskPriorityNumber)
                    : "",
            lruId: initialData?.lru?.id ? String(initialData.lru.id) : "",
            faultTemplateId: initialData?.faultTemplate?.id
                ? String(initialData.faultTemplate.id)
                : "",
        }),
        [initialData]
    );

    const [formState, setFormState] = useState(initialFormState);
    const [lrus, setLrus] = useState([]);
    const [faultTemplates, setFaultTemplates] = useState([]);
    const [loading, setLoading] = useState(true);
    const [loadingFaultTemplates, setLoadingFaultTemplates] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");
    const availableFaultTemplates = formState.lruId ? faultTemplates : [];

    useEffect(() => {
        const loadLrus = async () => {
            try {
                const response = await getLrus();
                setLrus(response.data);
            } catch (fetchError) {
                console.error("Failed to load LRUs:", fetchError);
                setError("Unable to load LRU options right now.");
            } finally {
                setLoading(false);
            }
        };

        loadLrus();
    }, []);

    useEffect(() => {
        if (!formState.lruId) {
            return;
        }

        const loadFaultTemplates = async () => {
            setLoadingFaultTemplates(true);

            try {
                const response =
                    await faultTemplateService.getFaultTemplatesByLruId(
                        formState.lruId
                    );
                setFaultTemplates(response.data);
            } catch (fetchError) {
                console.error("Failed to load fault templates:", fetchError);
                setError("Unable to load fault templates for this LRU.");
            } finally {
                setLoadingFaultTemplates(false);
            }
        };

        loadFaultTemplates();
    }, [formState.lruId]);

    const handleChange = (event) => {
        const { name, value } = event.target;
        setFormState((current) => ({
            ...current,
            [name]: value,
            ...(name === "lruId" ? { faultTemplateId: "" } : {}),
        }));
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError("");
        setSaving(true);

        const payload = {
            fmeaBranchCode: formState.fmeaBranchCode.trim(),
            failureEffect: formState.failureEffect.trim(),
            failureCause: formState.failureCause.trim(),
            detectionMethod: formState.detectionMethod.trim(),
            recommendedAction: formState.recommendedAction.trim(),
            riskPriorityNumber: formState.riskPriorityNumber
                ? Number(formState.riskPriorityNumber)
                : null,
            lru: {
                id: Number(formState.lruId),
            },
            faultTemplate: {
                id: Number(formState.faultTemplateId),
            },
        };

        try {
            if (initialData?.id) {
                await fmeaBranchMappingService.updateFmeaBranchMapping(
                    initialData.id,
                    payload
                );
            } else {
                await fmeaBranchMappingService.createFmeaBranchMapping(
                    payload
                );
            }

            if (onSuccess) {
                onSuccess();
            }
        } catch (saveError) {
            console.error("Failed to save FMEA branch mapping:", saveError);
            setError(
                "Could not save the FMEA branch mapping. Please review the form and try again."
            );
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return <p className="helper-text">Loading form options…</p>;
    }

    return (
        <form className="form-card" onSubmit={handleSubmit}>
            <div className="form-grid form-grid--two">
                <div className="form-field">
                    <label htmlFor="fmea-branch-code">
                        FMEA Branch Code
                    </label>
                    <input
                        id="fmea-branch-code"
                        type="text"
                        name="fmeaBranchCode"
                        placeholder="e.g. 3.2.1"
                        value={formState.fmeaBranchCode}
                        onChange={handleChange}
                        required
                    />
                </div>

                <div className="form-field">
                    <label htmlFor="fmea-rpn">Risk Priority Number</label>
                    <input
                        id="fmea-rpn"
                        type="number"
                        name="riskPriorityNumber"
                        min="1"
                        max="1000"
                        placeholder="1-1000"
                        value={formState.riskPriorityNumber}
                        onChange={handleChange}
                    />
                </div>

                <div className="form-field">
                    <label htmlFor="fmea-lru">LRU</label>
                    <select
                        id="fmea-lru"
                        name="lruId"
                        value={formState.lruId}
                        onChange={handleChange}
                        required
                    >
                        <option value="">Select LRU</option>
                        {lrus.map((item) => (
                            <option key={item.id} value={item.id}>
                                {item.name}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="form-field">
                    <label htmlFor="fmea-fault-template">
                        Fault Template
                    </label>
                    <select
                        id="fmea-fault-template"
                        name="faultTemplateId"
                        value={formState.faultTemplateId}
                        onChange={handleChange}
                        required
                        disabled={!formState.lruId || loadingFaultTemplates}
                    >
                        <option value="">
                            {formState.lruId
                                ? "Select fault template"
                                : "Select an LRU first"}
                        </option>
                        {availableFaultTemplates.map((item) => (
                            <option key={item.id} value={item.id}>
                                {item.faultCode} — {item.name}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="form-field" style={{ gridColumn: "1 / -1" }}>
                    <label htmlFor="fmea-failure-effect">
                        Failure Effect
                    </label>
                    <textarea
                        id="fmea-failure-effect"
                        name="failureEffect"
                        placeholder="Describe the observed failure effect"
                        value={formState.failureEffect}
                        onChange={handleChange}
                        rows={3}
                        required
                    />
                </div>

                <div className="form-field">
                    <label htmlFor="fmea-failure-cause">Failure Cause</label>
                    <textarea
                        id="fmea-failure-cause"
                        name="failureCause"
                        placeholder="Root cause of the failure"
                        value={formState.failureCause}
                        onChange={handleChange}
                        rows={3}
                    />
                </div>

                <div className="form-field">
                    <label htmlFor="fmea-detection-method">
                        Detection Method
                    </label>
                    <textarea
                        id="fmea-detection-method"
                        name="detectionMethod"
                        placeholder="How this failure is detected"
                        value={formState.detectionMethod}
                        onChange={handleChange}
                        rows={3}
                    />
                </div>

                <div
                    className="form-field"
                    style={{ gridColumn: "1 / -1" }}
                >
                    <label htmlFor="fmea-recommended-action">
                        Recommended Action
                    </label>
                    <textarea
                        id="fmea-recommended-action"
                        name="recommendedAction"
                        placeholder="Recommended corrective or mitigating action"
                        value={formState.recommendedAction}
                        onChange={handleChange}
                        rows={3}
                    />
                </div>
            </div>

            {error ? (
                <p style={{ color: "var(--danger)", marginTop: "12px" }}>
                    {error}
                </p>
            ) : null}

            <div className="form-actions">
                <button type="submit" disabled={saving}>
                    {saving
                        ? "Saving…"
                        : initialData
                            ? "Update FMEA Branch Mapping"
                            : "Create FMEA Branch Mapping"}
                </button>
            </div>
        </form>
    );
}
