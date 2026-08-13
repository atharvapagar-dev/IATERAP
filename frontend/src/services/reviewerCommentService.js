import api from "./api";

export const reviewerCommentService = {
    getByRevision(procedureHistoryId) {
        return api.get(`/reviewer-comments/by-revision/${procedureHistoryId}`);
    },

    createComment(comment) {
        return api.post("/reviewer-comments", comment);
    },
};
