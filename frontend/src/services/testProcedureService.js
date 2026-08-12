import api from "./api";

export const testProcedureService = {
    getAllProcedures() {
        return api.get("/procedures");
    },

    getProcedureById(id) {
        return api.get(`/procedures/${id}`);
    },

    createProcedure(procedure) {
        return api.post("/procedures", procedure);
    },

    updateProcedure(id, procedure) {
        return api.put(`/procedures/${id}`, procedure);
    },

    getHistory(id) {
        return api.get(`/procedures/${id}/history`);
    },

    getRevision(id, versionNumber) {
        return api.get(`/procedures/${id}/history/${versionNumber}`);
    },
};
