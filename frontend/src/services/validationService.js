import api from "./api";

export const validationService = {
    listRuns(procedureHistoryId) {
        return api.get("/validation/runs", { params: { procedureHistoryId } });
    },
    getRun(runId) {
        return api.get(`/validation/runs/${runId}`);
    },
    getReadings(runId) {
        return api.get(`/validation/runs/${runId}/readings`);
    },
    upload(procedureHistoryId, format, file) {
        const body = new FormData();
        body.append("procedureHistoryId", procedureHistoryId);
        body.append("format", format);
        body.append("file", file);
        return api.post("/validation/runs", body, {
            headers: { "Content-Type": "multipart/form-data" },
        });
    },
};
