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

    // AI-187 / T7-B3: non-destructive rollback. Never edits or removes
    // versionNumber or any other existing revision -- always appends a
    // brand new revision whose content matches the requested source
    // revision. Response is the live procedure (its versionNumber is the
    // newly created revision).
    rollbackToRevision(procedureId, versionNumber) {
        return api.post(`/procedures/${procedureId}/history/${versionNumber}/rollback`);
    },
};
