import api from "./api";

export const testStepService = {
    getStepById(id) {
        return api.get(`/steps/${id}`);
    },

    updateStep(id, step) {
        return api.put(`/steps/${id}`, step);
    },

    deleteStep(id) {
        return api.delete(`/steps/${id}`);
    },
};
