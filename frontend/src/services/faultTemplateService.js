import api from "./api";

export const faultTemplateService = {
    getAllFaultTemplates() {
        return api.get("/fault-templates");
    },

    getFaultTemplateById(id) {
        return api.get(`/fault-templates/${id}`);
    },

    getFaultTemplatesByLruId(lruId) {
        return api.get(`/fault-templates/by-lru/${lruId}`);
    },

    createFaultTemplate(faultTemplate) {
        return api.post("/fault-templates", faultTemplate);
    },

    updateFaultTemplate(id, faultTemplate) {
        return api.put(`/fault-templates/${id}`, faultTemplate);
    },

    deleteFaultTemplate(id) {
        return api.delete(`/fault-templates/${id}`);
    },
};
