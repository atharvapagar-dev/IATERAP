import api from "./api";

export const subsystemService = {
    getAllSubsystems() {
        return api.get("/subsystems");
    },

    createSubsystem(subsystem) {
        return api.post("/subsystems", subsystem);
    },
    getSubsystem(id) { return api.get(`/subsystems/${id}`); },
    updateSubsystem(id, subsystem) { return api.put(`/subsystems/${id}`, subsystem); },
    deleteSubsystem(id) { return api.delete(`/subsystems/${id}`); },
};
