import api from "./api";

export const interfaceService = {
    getAllInterfaces() {
        return api.get("/interfaces");
    },

    createInterface(techInterface) {
        return api.post("/interfaces", techInterface);
    },
};
