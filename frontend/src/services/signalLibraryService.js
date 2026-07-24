import api from "./api";

export const signalLibraryService = {
    getAllSignalLibraries() {
        return api.get("/signal-libraries");
    },

    getSignalLibraryById(id) {
        return api.get(`/signal-libraries/${id}`);
    },

    getSignalLibrariesByLruId(lruId) {
        return api.get(`/signal-libraries/by-lru/${lruId}`);
    },

    getSignalLibrariesByInterfaceId(interfaceId) {
        return api.get(`/signal-libraries/by-interface/${interfaceId}`);
    },

    createSignalLibrary(signalLibrary) {
        return api.post("/signal-libraries", signalLibrary);
    },

    updateSignalLibrary(id, signalLibrary) {
        return api.put(`/signal-libraries/${id}`, signalLibrary);
    },

    deleteSignalLibrary(id) {
        return api.delete(`/signal-libraries/${id}`);
    },
};
