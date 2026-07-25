import api from "./api";

export const protocolTagService = {
    getAllProtocolTags() {
        return api.get("/protocol-tags");
    },

    getProtocolTagById(id) {
        return api.get(`/protocol-tags/${id}`);
    },

    getProtocolTagsByLruId(lruId) {
        return api.get(`/protocol-tags/by-lru/${lruId}`);
    },

    createProtocolTag(protocolTag) {
        return api.post("/protocol-tags", protocolTag);
    },

    updateProtocolTag(id, protocolTag) {
        return api.put(`/protocol-tags/${id}`, protocolTag);
    },

    deleteProtocolTag(id) {
        return api.delete(`/protocol-tags/${id}`);
    },
};
