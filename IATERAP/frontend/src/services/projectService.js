import api from "./api";

export const projectService = {
  getAllProjects() {
    return api.get("/projects");
  },

  getProjectById(id) {
    return api.get(`/projects/${id}`);
  },

  createProject(project) {
    return api.post("/projects", project);
  },

  updateProject(id, project) {
    return api.put(`/projects/${id}`, project);
  },

  deleteProject(id) {
    return api.delete(`/projects/${id}`);
  },
};