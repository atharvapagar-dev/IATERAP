import api from "./api";

export const signalRangeService = {
    getSignalRangesBySignalLibraryId(signalLibraryId) {
        return api.get(
            `/signal-ranges/by-signal-library/${signalLibraryId}`
        );
    },
};
