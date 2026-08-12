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

    getSteps(procedureId) {
        return api.get(`/procedures/${procedureId}/steps`);
    },

    addStep(procedureId, step) {
        return api.post(`/procedures/${procedureId}/steps`, step);
    },

    reorderSteps(procedureId, orderedStepIds) {
        return api.patch(`/procedures/${procedureId}/steps/reorder`, orderedStepIds);
    },
};
