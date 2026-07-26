import api from "./api";

export const auditLogService = {
    getAllAuditLogs() {
        return api.get("/audit-logs");
    },

    getAuditLogsByEntityName(entityName) {
        return api.get(`/audit-logs/by-entity/${entityName}`);
    },

    getAuditLogsByEntityNameAndEntityId(entityName, entityId) {
        return api.get(`/audit-logs/by-entity/${entityName}/${entityId}`);
    },
};
