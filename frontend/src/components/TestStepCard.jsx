import { useMemo } from "react";

/**
 * PART K: reusable card for one typed TestStep. Renders the FK-bound
 * selects (Interface / Connector / Signal / Protocol / Fault Category),
 * the core engineering text fields, ordering controls, and approval
 * status -- using only fields that exist on the TestStep domain model
 * (see backend TestStepResponse). Options for Interface/Connector/
 * Signal/Protocol/Fault Category are pre-filtered by the procedure's
 * selected LRU (PART M) before being passed in as props, so this
 * component never needs to know about LRU filtering itself.
 */
export default function TestStepCard({
    step,
    index,
    total,
    interfaces,
    connectors,
    signalLibraries,
    protocolTags,
    faultTemplates,
    onChange,
    onRemove,
    onMoveUp,
    onMoveDown,
    errors = {},
}) {
    const filteredConnectors = useMemo(
        () =>
            step.interfaceId
                ? connectors.filter(
                      (c) => String(c.lru?.id) === String(step.interfaceId ? interfaces.find(i => String(i.id) === String(step.interfaceId))?.lru?.id : "")
                  )
                : connectors,
        [connectors, interfaces, step.interfaceId]
    );

    const handleField = (field) => (event) => {
        onChange({ ...step, [field]: event.target.value });
    };

    return (
        <div className="page-card stack test-step-card">
            <div
                className="test-step-card__header"
                style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}
            >
                <strong>Step {index + 1}</strong>

                <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
                    {step.approvalStatus ? (
                        <span className="helper-text">Status: {step.approvalStatus}</span>
                    ) : null}
                    <button
                        type="button"
                        onClick={onMoveUp}
                        disabled={index === 0}
                        title="Move up"
                    >
                        ↑
                    </button>
                    <button
                        type="button"
                        onClick={onMoveDown}
                        disabled={index === total - 1}
                        title="Move down"
                    >
                        ↓
                    </button>
                    <button type="button" onClick={onRemove} title="Remove step">
                        Remove
                    </button>
                </div>
            </div>

            <div className="form-grid form-grid--two">
                <div className="form-field">
                    <label>Interface</label>
                    <select
                        value={step.interfaceId || ""}
                        onChange={handleField("interfaceId")}
                        required
                    >
                        <option value="">Select interface</option>
                        {interfaces.map((i) => (
                            <option key={i.id} value={i.id}>
                                {i.name}
                            </option>
                        ))}
                    </select>
                    {errors.interfaceId ? (
                        <p style={{ color: "var(--danger)" }}>{errors.interfaceId}</p>
                    ) : null}
                </div>

                <div className="form-field">
                    <label>Connector</label>
                    <select
                        value={step.connectorId || ""}
                        onChange={handleField("connectorId")}
                        required
                    >
                        <option value="">Select connector</option>
                        {filteredConnectors.map((c) => (
                            <option key={c.id} value={c.id}>
                                {c.name}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="form-field">
                    <label>Signal</label>
                    <select
                        value={step.signalLibraryId || ""}
                        onChange={handleField("signalLibraryId")}
                        required
                    >
                        <option value="">Select signal</option>
                        {signalLibraries.map((s) => (
                            <option key={s.id} value={s.id}>
                                {s.name}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="form-field">
                    <label>Protocol</label>
                    <select
                        value={step.protocolTagId || ""}
                        onChange={handleField("protocolTagId")}
                        required
                    >
                        <option value="">Select protocol</option>
                        {protocolTags.map((p) => (
                            <option key={p.id} value={p.id}>
                                {p.name}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="form-field">
                    <label>Fault Category</label>
                    <select
                        value={step.faultCategoryId || ""}
                        onChange={handleField("faultCategoryId")}
                        required
                    >
                        <option value="">Select fault category</option>
                        {faultTemplates.map((f) => (
                            <option key={f.id} value={f.id}>
                                {f.name}
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            <div className="form-grid form-grid--two" style={{ marginTop: "12px" }}>
                <div className="form-field">
                    <label>Expected Behaviour</label>
                    <textarea
                        value={step.expectedBehaviour || ""}
                        onChange={handleField("expectedBehaviour")}
                        rows={2}
                        required
                    />
                    {errors.expectedBehaviour ? (
                        <p style={{ color: "var(--danger)" }}>{errors.expectedBehaviour}</p>
                    ) : null}
                </div>

                <div className="form-field">
                    <label>Expected Range</label>
                    <input
                        type="text"
                        value={step.expectedRange || ""}
                        onChange={handleField("expectedRange")}
                    />
                </div>

                <div className="form-field">
                    <label>Acceptance Criteria</label>
                    <textarea
                        value={step.acceptanceCriteria || ""}
                        onChange={handleField("acceptanceCriteria")}
                        rows={2}
                        required
                    />
                </div>

                <div className="form-field">
                    <label>Pass/Fail Criteria</label>
                    <textarea
                        value={step.passFailCriteria || ""}
                        onChange={handleField("passFailCriteria")}
                        rows={2}
                        required
                    />
                </div>
            </div>
        </div>
    );
}
