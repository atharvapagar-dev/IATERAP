import api from "./api";

export const subsystemService = {
    getAllSubsystems() {
        return api.get("/subsystems");
    },

    createSubsystem(subsystem) {
        return api.post("/subsystems", subsystem);
    },
};
