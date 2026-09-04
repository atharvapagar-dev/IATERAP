import api from "./api";

export const adminRuleConfigurationService = {
    getAllRules() {
        return api.get("/admin/rules");
    },

    setEnabled(ruleId, enabled) {
        return api.patch(`/admin/rules/${encodeURIComponent(ruleId)}/enabled`, {
            enabled,
        });
    },

    setPriority(ruleId, priority) {
        return api.patch(`/admin/rules/${encodeURIComponent(ruleId)}/priority`, {
            priority,
        });
    },
};
