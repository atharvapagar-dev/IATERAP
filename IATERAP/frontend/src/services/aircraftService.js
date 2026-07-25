import api from "./api";

export const aircraftService = {
    getAllAircraft() {
        return api.get("/aircraft");
    },

    createAircraft(aircraft) {
        return api.post("/aircraft", aircraft);
    },
};
