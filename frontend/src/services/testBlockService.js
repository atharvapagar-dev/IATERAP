import api from "./api";

export const testBlockService = {
    getAllBlocks() {
        return api.get("/test-blocks");
    },

    getBlockById(id) {
        return api.get(`/test-blocks/${id}`);
    },

    createFromStep(testStepId, payload) {
        return api.post(`/test-blocks/from-step/${testStepId}`, payload);
    },

    insertIntoProcedure(blockId, payload) {
        return api.post(`/test-blocks/${blockId}/insert`, payload);
    },
};