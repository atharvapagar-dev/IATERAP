import api from "./api";

export const fmeaBranchMappingService = {
    getAllFmeaBranchMappings() {
        return api.get("/fmea-branch-mappings");
    },

    getFmeaBranchMappingById(id) {
        return api.get(`/fmea-branch-mappings/${id}`);
    },

    getFmeaBranchMappingsByLruId(lruId) {
        return api.get(`/fmea-branch-mappings/by-lru/${lruId}`);
    },

    getFmeaBranchMappingsByFaultTemplateId(faultTemplateId) {
        return api.get(
            `/fmea-branch-mappings/by-fault-template/${faultTemplateId}`
        );
    },

    createFmeaBranchMapping(fmeaBranchMapping) {
        return api.post("/fmea-branch-mappings", fmeaBranchMapping);
    },

    updateFmeaBranchMapping(id, fmeaBranchMapping) {
        return api.put(`/fmea-branch-mappings/${id}`, fmeaBranchMapping);
    },

    deleteFmeaBranchMapping(id) {
        return api.delete(`/fmea-branch-mappings/${id}`);
    },
};
