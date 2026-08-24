import api from "./api";

export const versionComparisonService = {
    getHistory(procedureId) {
        return api.get(`/procedures/${procedureId}/history`);
    },

    compareVersions(procedureId, fromVersion, toVersion) {
        return api.get(`/procedures/${procedureId}/compare`, {
            params: { fromVersion, toVersion },
        });
    },
};
