import { useEffect, useState } from "react";

import ManagementLayout from "../components/ManagementLayout";
import { adminRuleConfigurationService } from "../services/adminRuleConfigurationService";

const accessMessage =
    "This console is available only to administrators. Sign in with an ADMIN account to manage rules.";

function errorMessage(error, fallback) {
    if (error.response?.status === 403) {
        return accessMessage;
    }

    return error.response?.data?.message || fallback;
}

export default function AdminRuleConfigurationPage() {
    const [rules, setRules] = useState([]);
    const [priorityDrafts, setPriorityDrafts] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [savingRuleId, setSavingRuleId] = useState("");

    const loadRules = async () => {
        try {
            setLoading(true);
            setError("");
            const response = await adminRuleConfigurationService.getAllRules();
            const configuredRules = response.data || [];
            setRules(configuredRules);
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
        void loadRules();
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
            description="Configure which engineering rules participate in procedure validation and the deterministic order in which they run."
        >
            <section className="page-card stack">
                <div className="page-toolbar">
                    <div>
                        <p className="eyebrow">ADMIN CONSOLE / T9-B3</p>
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

                {error ? <p className="rule-management__error">{error}</p> : null}

                {loading ? (
                    <p className="helper-text">Loading configured rules...</p>
                ) : rules.length ? (
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
                                {rules.map((rule) => {
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
