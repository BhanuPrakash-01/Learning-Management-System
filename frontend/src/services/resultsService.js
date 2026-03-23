import api from "../api/axios";

export const getStudentResults = () => api.get("/student/results");
