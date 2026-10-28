import api from "./api";

const unwrap = (request) => request.then((response) => ({ ...response, data: response.data.data }));

export const adminRuleConfigurationService = {
    getAllRules() {
        return unwrap(api.get("/admin/rules"));
    },

    setEnabled(ruleId, enabled) {
        return unwrap(api.patch(`/admin/rules/${encodeURIComponent(ruleId)}/enabled`, {
            enabled,
        }));
    },

    setPriority(ruleId, priority) {
        return unwrap(api.patch(`/admin/rules/${encodeURIComponent(ruleId)}/priority`, {
            priority,
        }));
    },

    getCoverageScoringConfiguration() {
        return api.get("/admin/coverage/scoring-config");
    },

    updateCoverageScoringConfiguration(rules) {
        return api.put("/admin/coverage/scoring-config", { rules });
    },
};
