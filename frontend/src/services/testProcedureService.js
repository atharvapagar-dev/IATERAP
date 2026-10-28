import api from "./api";

const unwrap = (request) => request.then((response) => ({ ...response, data: response.data.data }));

export const testProcedureService = {
    getAllProcedures() {
        return unwrap(api.get("/procedures"));
    },

    getProcedureById(id) {
        return unwrap(api.get(`/procedures/${id}`));
    },

    getProceduresByLruId(lruId) {
        return unwrap(api.get(`/procedures/by-lru/${lruId}`));
    },

    createProcedure(procedure) {
        return unwrap(api.post("/procedures", procedure));
    },

    updateProcedure(id, procedure) {
        return unwrap(api.put(`/procedures/${id}`, procedure));
    },

    deleteProcedure(id) {
        return api.delete(`/procedures/${id}`);
    },

    submitForReview(id) {
        return unwrap(api.post(`/procedures/${id}/submit-for-review`));
    },

    approve(id) {
        return unwrap(api.post(`/procedures/${id}/approve`));
    },

    reject(id) {
        return unwrap(api.post(`/procedures/${id}/reject`));
    },

    getReviewQueue() {
        return unwrap(api.get(`/procedures/review-queue`));
    },

    getSteps(procedureId) {
        return api.get(`/steps/by-procedure/${procedureId}`);
    },

    getCoverage(procedureId) {
        return unwrap(api.get(`/procedures/${procedureId}/coverage`));
    },

    addStep(procedureId, step) {
        return api.post(`/steps/by-procedure/${procedureId}`, step);
    },

    reorderSteps(procedureId, orderedStepIds) {
        return api.patch(`/steps/by-procedure/${procedureId}/reorder`, orderedStepIds);
    },

    getHistory(id) {
        return unwrap(api.get(`/procedures/${id}/history`));
    },

    getRevision(id, versionNumber) {
        return unwrap(api.get(`/procedures/${id}/history/${versionNumber}`));
    },
};
