import { useEffect, useState } from "react";

import ManagementLayout from "../components/ManagementLayout";
import { adminRuleConfigurationService } from "../services/adminRuleConfigurationService";

const accessMessage =
    "This console is available only to administrators. Sign in with an ADMIN account to manage rules.";

const coverageOrder = [
    "COVERAGE-INTERFACE",
    "COVERAGE-SIGNAL",
    "COVERAGE-REQUIREMENT",
    "COVERAGE-FUNCTIONAL",
    "COVERAGE-FAULT",
    "COVERAGE-FMEA",
];

const coverageLabels = {
    "COVERAGE-INTERFACE": "Interface",
    "COVERAGE-SIGNAL": "Signal",
    "COVERAGE-REQUIREMENT": "Requirement",
    "COVERAGE-FUNCTIONAL": "Functional",
    "COVERAGE-FAULT": "Fault",
    "COVERAGE-FMEA": "FMEA",
};

function errorMessage(error, fallback) {
    if (error.response?.status === 403) {
        return accessMessage;
    }

    return error.response?.data?.message || fallback;
}

export default function AdminRuleConfigurationPage() {
    const [rules, setRules] = useState([]);
    const [priorityDrafts, setPriorityDrafts] = useState({});
    const [coverageDrafts, setCoverageDrafts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [savingRuleId, setSavingRuleId] = useState("");
    const [savingCoverage, setSavingCoverage] = useState(false);

    const loadRules = async () => {
        try {
            setLoading(true);
            setError("");
            const [rulesResponse, coverageResponse] = await Promise.all([
                adminRuleConfigurationService.getAllRules(),
                adminRuleConfigurationService.getCoverageScoringConfiguration(),
            ]);
            const configuredRules = rulesResponse.data || [];
            setRules(configuredRules);
            setCoverageDrafts(
                [...(coverageResponse.data || [])]
                    .sort((left, right) => coverageOrder.indexOf(left.ruleId) - coverageOrder.indexOf(right.ruleId))
                    .map((rule) => ({
                        ruleId: rule.ruleId,
                        enabled: rule.enabled,
                        weight: String(rule.weight ?? ""),
                        threshold: String(rule.threshold ?? ""),
                    }))
            );
            setPriorityDrafts(
                Object.fromEntries(
                    configuredRules.map((rule) => [rule.ruleId, String(rule.priority)])
                )
            );
        } catch (fetchError) {
            console.error("Failed to fetch rule configuration:", fetchError);
            setError(errorMessage(fetchError, "Unable to load rule configuration right now."));
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        const loadTimer = window.setTimeout(() => {
            void loadRules();
        }, 0);

        return () => window.clearTimeout(loadTimer);
    }, []);

    const replaceRule = (updatedRule) => {
        setRules((currentRules) =>
            currentRules.map((rule) =>
                rule.ruleId === updatedRule.ruleId ? updatedRule : rule
            )
        );
        setPriorityDrafts((currentDrafts) => ({
            ...currentDrafts,
            [updatedRule.ruleId]: String(updatedRule.priority),
        }));
    };

    const updateEnabled = async (rule) => {
        try {
            setSavingRuleId(rule.ruleId);
            setError("");
            const response = await adminRuleConfigurationService.setEnabled(
                rule.ruleId,
                !rule.enabled
            );
            replaceRule(response.data);
        } catch (updateError) {
            console.error("Failed to update rule state:", updateError);
            setError(errorMessage(updateError, "Unable to update this rule right now."));
        } finally {
            setSavingRuleId("");
        }
    };

    const updateCoverageDraft = (ruleId, field, value) => {
        setCoverageDrafts((currentDrafts) =>
            currentDrafts.map((rule) =>
                rule.ruleId === ruleId ? { ...rule, [field]: value } : rule
            )
        );
    };

    const enabledWeightTotal = coverageDrafts.reduce((total, rule) => {
        const weight = Number(rule.weight);
        return rule.enabled && Number.isFinite(weight) ? total + weight : total;
    }, 0);

    const saveCoverageConfiguration = async (event) => {
        event.preventDefault();
        setSuccess("");

        const invalidValue = coverageDrafts.some((rule) => {
            const weight = Number(rule.weight);
            const threshold = Number(rule.threshold);
            return !Number.isFinite(weight) || !Number.isFinite(threshold) ||
                weight < 0 || weight > 100 || threshold < 0 || threshold > 100 ||
                !/^\d+(\.\d{1,2})?$/.test(rule.weight) || !/^\d+(\.\d{1,2})?$/.test(rule.threshold);
        });

        if (invalidValue) {
            setError("Weights and thresholds must be percentages from 0.00 to 100.00 with at most two decimal places.");
            return;
        }

        if (Math.abs(enabledWeightTotal - 100) > 0.000001) {
            setError("Enabled coverage weights must total exactly 100%.");
            return;
        }

        try {
            setSavingCoverage(true);
            setError("");
            await adminRuleConfigurationService.updateCoverageScoringConfiguration(
                coverageDrafts.map((rule) => ({
                    ...rule,
                    weight: Number(rule.weight),
                    threshold: Number(rule.threshold),
                }))
            );
            setSuccess("Coverage scoring configuration saved.");
            await loadRules();
        } catch (updateError) {
            console.error("Failed to update coverage scoring configuration:", updateError);
            setError(errorMessage(updateError, "Unable to save coverage scoring configuration right now."));
        } finally {
            setSavingCoverage(false);
        }
    };

    const updatePriority = async (event, rule) => {
        event.preventDefault();
        const priority = Number(priorityDrafts[rule.ruleId]);

        if (!Number.isInteger(priority) || priority < 0) {
            setError("Priority must be a whole number that is zero or greater.");
            return;
        }

        try {
            setSavingRuleId(rule.ruleId);
            setError("");
            const response = await adminRuleConfigurationService.setPriority(
                rule.ruleId,
                priority
            );
            replaceRule(response.data);
        } catch (updateError) {
            console.error("Failed to update rule priority:", updateError);
            setError(errorMessage(updateError, "Unable to update rule priority right now."));
        } finally {
            setSavingRuleId("");
        }
    };

    return (
        <ManagementLayout
            title="Rule Management"
            description="Configure engineering rule execution and the runtime coverage scoring configuration."
        >
            <section className="page-card stack">
                <div className="page-toolbar">
                    <div>
                        <p className="eyebrow">ADMIN CONSOLE / T9-B3 + T10-B3</p>
                        <p className="helper-text">
                            Rule implementations remain code-owned; this console manages their persisted enabled state and priority.
                        </p>
                    </div>
                    <button
                        className="button button--secondary"
                        disabled={loading || Boolean(savingRuleId)}
                        onClick={() => void loadRules()}
                    >
                        Refresh rules
                    </button>
                </div>

                {error ? <p className="rule-management__error" role="alert">{error}</p> : null}
                {success ? <p className="rule-management__success" role="status">{success}</p> : null}

                {loading ? (
                    <p className="helper-text">Loading configured rules and coverage scoring...</p>
                ) : (
                    <form className="coverage-scoring" onSubmit={saveCoverageConfiguration}>
                        <div className="page-toolbar">
                            <div>
                                <h2 className="section-title">Coverage scoring</h2>
                                <p className="helper-text">Only enabled categories contribute to the runtime composite. No weights are adjusted automatically.</p>
                            </div>
                            <p className={`coverage-scoring__total ${Math.abs(enabledWeightTotal - 100) < 0.000001 ? "coverage-scoring__total--valid" : ""}`}>
                                Enabled total: {enabledWeightTotal.toFixed(2)}%
                            </p>
                        </div>

                        <div className="rule-management__table-wrap">
                            <table className="entity-table">
                                <thead>
                                    <tr>
                                        <th>Coverage category</th>
                                        <th>Enabled</th>
                                        <th>Weight (%)</th>
                                        <th>Threshold (%)</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {coverageDrafts.map((rule) => (
                                        <tr key={rule.ruleId}>
                                            <td><strong>{coverageLabels[rule.ruleId] || rule.ruleId}</strong><br /><code>{rule.ruleId}</code></td>
                                            <td><input aria-label={`Enabled ${coverageLabels[rule.ruleId] || rule.ruleId}`} checked={rule.enabled} onChange={(event) => updateCoverageDraft(rule.ruleId, "enabled", event.target.checked)} type="checkbox" /></td>
                                            <td><input aria-label={`Weight for ${coverageLabels[rule.ruleId] || rule.ruleId}`} inputMode="decimal" min="0" max="100" onChange={(event) => updateCoverageDraft(rule.ruleId, "weight", event.target.value)} step="0.01" type="number" value={rule.weight} /></td>
                                            <td><input aria-label={`Threshold for ${coverageLabels[rule.ruleId] || rule.ruleId}`} inputMode="decimal" min="0" max="100" onChange={(event) => updateCoverageDraft(rule.ruleId, "threshold", event.target.value)} step="0.01" type="number" value={rule.threshold} /></td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        <div className="form-actions">
                            <button disabled={savingCoverage || !coverageDrafts.length} type="submit">
                                {savingCoverage ? "Saving coverage configuration..." : "Save coverage configuration"}
                            </button>
                        </div>
                    </form>
                )}

                {loading ? (
                    null
                ) : rules.filter((rule) => rule.category !== "COVERAGE").length ? (
                    <div className="rule-management__table-wrap">
                        <table className="entity-table">
                            <thead>
                                <tr>
                                    <th>Rule identifier</th>
                                    <th>Category</th>
                                    <th>Description</th>
                                    <th>Priority</th>
                                    <th>State</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {rules.filter((rule) => rule.category !== "COVERAGE").map((rule) => {
                                    const saving = savingRuleId === rule.ruleId;

                                    return (
                                        <tr key={rule.ruleId}>
                                            <td><code>{rule.ruleId}</code></td>
                                            <td><span className="status-pill">{rule.category}</span></td>
                                            <td>{rule.description}</td>
                                            <td>
                                                <form
                                                    className="rule-management__priority-form"
                                                    onSubmit={(event) => updatePriority(event, rule)}
                                                >
                                                    <input
                                                        aria-label={`Priority for ${rule.ruleId}`}
                                                        min="0"
                                                        onChange={(event) =>
                                                            setPriorityDrafts((currentDrafts) => ({
                                                                ...currentDrafts,
                                                                [rule.ruleId]: event.target.value,
                                                            }))
                                                        }
                                                        type="number"
                                                        value={priorityDrafts[rule.ruleId] ?? ""}
                                                    />
                                                    <button disabled={saving} type="submit">
                                                        Save
                                                    </button>
                                                </form>
                                            </td>
                                            <td>
                                                <span className="status-pill">
                                                    {rule.enabled ? "ENABLED" : "DISABLED"}
                                                </span>
                                            </td>
                                            <td>
                                                <div className="table-actions">
                                                    <button
                                                        className={rule.enabled ? "button button--danger" : "button button--secondary"}
                                                        disabled={saving}
                                                        onClick={() => void updateEnabled(rule)}
                                                    >
                                                        {rule.enabled ? "Disable" : "Enable"}
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    <div className="empty-state">
                        No rules are configured. Apply the database migrations before managing engineering rules.
                    </div>
                )}
            </section>
        </ManagementLayout>
    );
}
