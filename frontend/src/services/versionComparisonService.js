import api from "./api";

const unwrap = (request) => request.then((response) => ({ ...response, data: response.data.data }));

export const versionComparisonService = {
    getHistory(procedureId) {
        return unwrap(api.get(`/procedures/${procedureId}/history`));
    },

    compareVersions(procedureId, fromVersion, toVersion) {
        return api.get(`/procedures/${procedureId}/compare`, {
            params: { fromVersion, toVersion },
        });
    },

    rollbackToRevision(procedureId, versionNumber) {
        return unwrap(api.post(`/procedures/${procedureId}/history/${versionNumber}/rollback`));
    },
};
