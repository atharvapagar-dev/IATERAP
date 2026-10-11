import api from "./api";

export const syntheticEventService = {
    generate(aircraftId) {
        return api.post("/reliability/synthetic-events/generate", null, { params: { aircraftId } });
    },
};
