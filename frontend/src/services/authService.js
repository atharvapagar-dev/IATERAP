import api from "./api";

export const authService = {
  login(credentials) {
    return api.post("/auth/login", credentials);
  },

  refreshToken(refreshToken) {
    return api.post("/auth/refresh", { refreshToken });
  },

  logout() {
    return Promise.resolve();
  },
};