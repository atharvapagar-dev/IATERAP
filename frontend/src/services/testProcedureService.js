import api from "./api";

export const testProcedureService = {
    getAllProcedures() {
        return api.get("/procedures");
    },

    getProcedureById(id) {
        return api.get(`/procedures/${id}`);
    },

    getProceduresByLruId(lruId) {
        return api.get(`/procedures/by-lru/${lruId}`);
    },

    createProcedure(procedure) {
        return api.post("/procedures", procedure);
    },

    updateProcedure(id, procedure) {
        return api.put(`/procedures/${id}`, procedure);
    },

    deleteProcedure(id) {
        return api.delete(`/procedures/${id}`);
    },

    submitForReview(id) {
        return api.post(`/procedures/${id}/submit-for-review`);
    },

    getSteps(procedureId) {
        return api.get(`/procedures/${procedureId}/steps`);
    },

    addStep(procedureId, step) {
        return api.post(`/procedures/${procedureId}/steps`, step);
    },

    reorderSteps(procedureId, orderedStepIds) {
        return api.patch(`/procedures/${procedureId}/steps/reorder`, orderedStepIds);
    },

    getHistory(id) {
        return api.get(`/procedures/${id}/history`);
    },

    getRevision(id, versionNumber) {
        return api.get(`/procedures/${id}/history/${versionNumber}`);
    },
};
