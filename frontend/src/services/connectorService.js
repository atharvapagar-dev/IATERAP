import api from "./api";
export const connectorService = {
    getAllConnectors() {
        return api.get("/connectors");
    },
    getConnectorsByLruId(lruId) {
        return api.get(`/connectors/by-lru/${lruId}`);
    },
    createConnector(connector) {
        return api.post("/connectors", connector);
    },
};
