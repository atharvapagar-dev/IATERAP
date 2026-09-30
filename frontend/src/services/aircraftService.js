import api from "./api";

export const aircraftService = {
    getAllAircraft() {
        return api.get("/aircraft");
    },

    createAircraft(aircraft) {
        return api.post("/aircraft", aircraft);
    },
    getAircraft(id) { return api.get(`/aircraft/${id}`); },
    updateAircraft(id, aircraft) { return api.put(`/aircraft/${id}`, aircraft); },
    deleteAircraft(id) { return api.delete(`/aircraft/${id}`); },
};
