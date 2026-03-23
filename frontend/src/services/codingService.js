import api from "../api/axios";

export const getCodingProblems = (params = {}) => api.get("/student/coding/problems", { params });
export const submitCodingSolution = (data) => api.post("/student/coding/submit", data);
