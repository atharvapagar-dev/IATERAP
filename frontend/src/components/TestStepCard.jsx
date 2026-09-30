import { useEffect, useState } from "react";

import { interfaceService } from "../services/interfaceService";
import { connectorService } from "../services/connectorService";
import { signalLibraryService } from "../services/signalLibraryService";
import { protocolTagService } from "../services/protocolTagService";
import { faultTemplateService } from "../services/faultTemplateService";

export default function TestStepCard({
    step,
    lruId,
    readOnly = false,
    onSave,
    onDelete,
    onMoveUp,
    onMoveDown,
    isFirst = false,
    isLast = false,
}) {
    const isNew = !step?.id;

    const [formState, setFormState] = useState({
        interfaceId: step?.interfaceEntity?.id ? String(step.interfaceEntity.id) : "",
        connectorId: step?.connector?.id ? String(step.connector.id) : "",
        signalLibraryId: step?.signalLibrary?.id ? String(step.signalLibrary.id) : "",
        protocolTagId: step?.protocolTag?.id ? String(step.protocolTag.id) : "",
        faultTemplateId: step?.faultCategory?.id ? String(step.faultCategory.id) : "",
        expectedBehaviour: step?.expectedBehaviour || "",
        expectedRange: step?.expectedRange || "",
        calibrationRequirement: step?.calibrationRequirement || "",
        safetyNotes: step?.safetyNotes || "",
        acceptanceCriteria: step?.acceptanceCriteria || "",
        preconditions: step?.preconditions || "",
        dependencies: step?.dependencies || "",
        postConditions: step?.postConditions || "",
        passFailCriteria: step?.passFailCriteria || "",
        expectedObservation: step?.expectedObservation || "",
        engineerRemarks: step?.engineerRemarks || "",
    });

    const [interfaces, setInterfaces] = useState([]);
    const [connectors, setConnectors] = useState([]);
    const [signalLibraries, setSignalLibraries] = useState([]);
    const [protocolTags, setProtocolTags] = useState([]);
    const [faultTemplates, setFaultTemplates] = useState([]);
    const [loadingOptions, setLoadingOptions] = useState(!readOnly && Boolean(lruId));
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        if (readOnly || !lruId) {
            return;
        }

        const loadOptions = async () => {
            try {
                const [ifaceRes, connRes, sigRes, protoRes, faultRes] = await Promise.all([
                    interfaceService.getInterfacesByLruId(lruId),
                    connectorService.getConnectorsByLruId(lruId),
                    signalLibraryService.getSignalLibrariesByLruId(lruId),
                    protocolTagService.getProtocolTagsByLruId(lruId),
                    faultTemplateService.getFaultTemplatesByLruId(lruId),
                ]);
                setInterfaces(ifaceRes.data);
                setConnectors(connRes.data);
                setSignalLibraries(sigRes.data);
                setProtocolTags(protoRes.data);
                setFaultTemplates(faultRes.data);
            } catch (fetchError) {
                console.error("Failed to load Test Step reference options:", fetchError);
                setError("Unable to load Interface/Connector/Signal/Protocol/Fault options for this LRU.");
            } finally {
                setLoadingOptions(false);
            }
        };

        loadOptions();
    }, [lruId, readOnly]);

    const handleChange = (event) => {
        const { name, value } = event.target;
        setFormState((current) => ({ ...current, [name]: value }));
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError("");
        setSaving(true);

        const payload = {
            interfaceEntity: formState.interfaceId ? { id: Number(formState.interfaceId) } : null,
            connector: formState.connectorId ? { id: Number(formState.connectorId) } : null,
            signalLibrary: formState.signalLibraryId ? { id: Number(formState.signalLibraryId) } : null,
            protocolTag: formState.protocolTagId ? { id: Number(formState.protocolTagId) } : null,
            faultCategory: formState.faultTemplateId ? { id: Number(formState.faultTemplateId) } : null,
            expectedBehaviour: formState.expectedBehaviour.trim(),
            expectedRange: formState.expectedRange.trim(),
            calibrationRequirement: formState.calibrationRequirement.trim(),
            safetyNotes: formState.safetyNotes.trim(),
            acceptanceCriteria: formState.acceptanceCriteria.trim(),
            preconditions: formState.preconditions.trim(),
            dependencies: formState.dependencies.trim(),
            postConditions: formState.postConditions.trim(),
            passFailCriteria: formState.passFailCriteria.trim(),
            expectedObservation: formState.expectedObservation.trim(),
            engineerRemarks: formState.engineerRemarks.trim(),
        };

        try {
            await onSave(payload);
            if (isNew) {
                setFormState((current) => ({
                    ...current,
                    expectedBehaviour: "",
                    acceptanceCriteria: "",
                    passFailCriteria: "",
                }));
            }
        } catch (saveError) {
            console.error("Failed to save Test Step:", saveError);
            setError("Could not save this Test Step. Please review the form and try again.");
        } finally {
            setSaving(false);
        }
    };

    if (readOnly) {
        return (
            <div className="page-card">
                <p><strong>Step {step.executionOrder}</strong> - {step.approvalStatus}</p>
                <p>{step.expectedBehaviour}</p>
                <p className="helper-text">Acceptance: {step.acceptanceCriteria}</p>
                <p className="helper-text">Pass/Fail: {step.passFailCriteria}</p>
            </div>
        );
    }

    if (loadingOptions) {
        return <p className="helper-text">Loading step options...</p>;
    }

    return (
        <form className="form-card" onSubmit={handleSubmit}>
            {!isNew ? (
                <div className="page-toolbar">
                    <strong>Step {step.executionOrder}</strong>
                    <div className="table-actions">
                        <button type="button" onClick={onMoveUp} disabled={isFirst}>Move Up</button>
                        <button type="button" onClick={onMoveDown} disabled={isLast}>Move Down</button>
                        <button type="button" className="button--danger" onClick={() => onDelete(step.id)}>
                            Delete
                        </button>
                    </div>
                </div>
            ) : (
                <p className="helper-text">Add a new Test Step</p>
            )}

            <div className="form-grid form-grid--two">
                <div className="form-field">
                    <label>Interface</label>
                    <select name="interfaceId" value={formState.interfaceId} onChange={handleChange}>
                        <option value="">None</option>
                        {interfaces.map((i) => (
                            <option key={i.id} value={i.id}>{i.name}</option>
                        ))}
                    </select>
                </div>

                <div className="form-field">
                    <label>Connector</label>
                    <select name="connectorId" value={formState.connectorId} onChange={handleChange}>
                        <option value="">None</option>
                        {connectors.map((c) => (
                            <option key={c.id} value={c.id}>{c.name}</option>
                        ))}
                    </select>
                </div>

                <div className="form-field">
                    <label>Signal Library</label>
                    <select name="signalLibraryId" value={formState.signalLibraryId} onChange={handleChange}>
                        <option value="">None</option>
                        {signalLibraries.map((s) => (
                            <option key={s.id} value={s.id}>{s.name}</option>
                        ))}
                    </select>
                </div>

                <div className="form-field">
                    <label>Protocol Tag</label>
                    <select name="protocolTagId" value={formState.protocolTagId} onChange={handleChange}>
                        <option value="">None</option>
                        {protocolTags.map((p) => (
                            <option key={p.id} value={p.id}>{p.name}</option>
                        ))}
                    </select>
                </div>

                <div className="form-field">
                    <label>Fault Category</label>
                    <select name="faultTemplateId" value={formState.faultTemplateId} onChange={handleChange}>
                        <option value="">None</option>
                        {faultTemplates.map((f) => (
                            <option key={f.id} value={f.id}>{f.name}</option>
                        ))}
                    </select>
                </div>
            </div>

            <div className="form-grid form-grid--two" style={{ marginTop: "16px" }}>
                <div className="form-field">
                    <label>Expected Behaviour *</label>
                    <textarea name="expectedBehaviour" value={formState.expectedBehaviour} onChange={handleChange} required rows={2} />
                </div>
                <div className="form-field">
                    <label>Expected Range</label>
                    <input type="text" name="expectedRange" value={formState.expectedRange} onChange={handleChange} />
                </div>
                <div className="form-field">
                    <label>Acceptance Criteria *</label>
                    <textarea name="acceptanceCriteria" value={formState.acceptanceCriteria} onChange={handleChange} required rows={2} />
                </div>
                <div className="form-field">
                    <label>Pass/Fail Criteria *</label>
                    <textarea name="passFailCriteria" value={formState.passFailCriteria} onChange={handleChange} required rows={2} />
                </div>
                <div className="form-field">
                    <label>Calibration Requirement</label>
                    <input type="text" name="calibrationRequirement" value={formState.calibrationRequirement} onChange={handleChange} />
                </div>
                <div className="form-field">
                    <label>Safety Notes</label>
                    <input type="text" name="safetyNotes" value={formState.safetyNotes} onChange={handleChange} />
                </div>
                <div className="form-field">
                    <label>Preconditions</label>
                    <textarea name="preconditions" value={formState.preconditions} onChange={handleChange} rows={2} />
                </div>
                <div className="form-field">
                    <label>Dependencies</label>
                    <textarea name="dependencies" value={formState.dependencies} onChange={handleChange} rows={2} />
                </div>
                <div className="form-field">
                    <label>Post Conditions</label>
                    <textarea name="postConditions" value={formState.postConditions} onChange={handleChange} rows={2} />
                </div>
                <div className="form-field">
                    <label>Expected Observation</label>
                    <textarea name="expectedObservation" value={formState.expectedObservation} onChange={handleChange} rows={2} />
                </div>
                <div className="form-field">
                    <label>Engineer Remarks</label>
                    <textarea name="engineerRemarks" value={formState.engineerRemarks} onChange={handleChange} rows={2} />
                </div>
            </div>

            {error ? <p style={{ color: "var(--danger)", marginTop: "12px" }}>{error}</p> : null}

            <div className="form-actions">
                <button type="submit" disabled={saving}>
                    {saving ? "Saving..." : isNew ? "Add Step" : "Save Step"}
                </button>
            </div>
        </form>
    );
}
