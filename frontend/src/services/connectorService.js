import api from "./api";

export const connectorService = {
    getAllConnectors() {
        return api.get("/connectors");
    },

    getConnectorById(id) {
        return api.get(`/connectors/${id}`);
    },
};
