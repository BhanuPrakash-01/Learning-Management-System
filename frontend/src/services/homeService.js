import api from "../api/axios";

export const getStudentHome = () => api.get("/student/home");
