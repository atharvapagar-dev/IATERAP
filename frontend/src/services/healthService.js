import api from "./api";

export const healthService = {
  checkHealth() {
    return api.get("/health");
  },
};