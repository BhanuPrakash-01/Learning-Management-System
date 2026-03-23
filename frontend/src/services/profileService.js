import api from "../api/axios";

export const getMyProfile = () => api.get("/student/profile");
export const updateMyProfile = (data) => api.patch("/student/profile", data);
