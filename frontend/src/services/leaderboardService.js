import api from "../api/axios";

export const getLeaderboard = (params = {}) => api.get("/student/leaderboard", { params });
