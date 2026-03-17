import api from "../api/axios";

// ---- Courses ----
export const getAdminCourses = () => api.get("/admin/courses");
export const createCourse = (data) => api.post("/admin/courses", data);
export const updateCourse = (id, data) => api.put(`/admin/courses/${id}`, data);
export const deleteCourse = (id) => api.delete(`/admin/courses/${id}`);

// ---- Assessments ----
export const getAdminAssessments = () => api.get("/admin/assessments");
export const createAssessment = (data) => api.post("/admin/assessments", data);
export const updateAssessment = (id, data) => api.put(`/admin/assessments/${id}`, data);
export const deleteAssessment = (id) => api.delete(`/admin/assessments/${id}`);

// ---- Questions ----
export const getAdminQuestions = () => api.get("/admin/questions");
export const getQuestionsByAssessment = (assessmentId) => api.get(`/admin/questions/assessment/${assessmentId}`);
export const createQuestion = (data) => api.post("/admin/questions", data);
export const updateQuestion = (id, data) => api.put(`/admin/questions/${id}`, data);
export const deleteQuestion = (id) => api.delete(`/admin/questions/${id}`);
