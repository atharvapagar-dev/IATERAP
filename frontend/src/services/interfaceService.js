import api from "./api";
export const interfaceService = {
    getAllInterfaces() {
        return api.get("/interfaces");
    },
    getInterfacesByLruId(lruId) {
        return api.get(`/interfaces/by-lru/${lruId}`);
    },
    createInterface(techInterface) {
        return api.post("/interfaces", techInterface);
    },
};
