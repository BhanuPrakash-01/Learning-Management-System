import api from "../api/axios";

export const getDashboardStats = () => api.get("/admin/dashboard/stats");

export const getStudents = (params = {}) => api.get("/admin/students", { params });
export const getStudentDetail = (id) => api.get(`/admin/students/${id}`);
export const deactivateStudent = (id) => api.patch(`/admin/students/${id}/deactivate`);
export const resetStudentPassword = (id) => api.patch(`/admin/students/${id}/reset-password`);

export const getAdminAssessments = () => api.get("/admin/assessments");
export const createAssessment = (data) => api.post("/admin/assessments", data);
export const updateAssessment = (id, data) => api.put(`/admin/assessments/${id}`, data);
export const deleteAssessment = (id) => api.delete(`/admin/assessments/${id}`);

export const getAdminQuestions = () => api.get("/admin/questions");
export const getQuestionsByAssessment = (assessmentId) =>
  api.get(`/admin/questions/assessment/${assessmentId}`);
export const getQuestionLibrary = (params = {}) => api.get("/admin/questions/library", { params });
export const createQuestion = (data) => api.post("/admin/questions", data);
export const updateQuestion = (id, data) => api.put(`/admin/questions/${id}`, data);
export const deleteQuestion = (id) => api.delete(`/admin/questions/${id}`);

export const bulkUploadQuestions = (file) => {
  const formData = new FormData();
  formData.append("file", file);
  return api.post("/admin/questions/bulk-upload", formData);
};

export const getPracticeCategories = () => api.get("/admin/practice/categories");
export const createPracticeCategory = (data) => api.post("/admin/practice/categories", data);
export const createPracticeTopic = (data) => api.post("/admin/practice/topics", data);
export const bulkUploadPracticeQuestions = (file) => {
  const formData = new FormData();
  formData.append("file", file);
  return api.post("/admin/practice/questions/bulk", formData);
};

export const getCodingProblemsAdmin = () => api.get("/admin/coding/problems");
export const createCodingProblem = (data) => api.post("/admin/coding/problems", data);

export const getLeaderboardConfig = () => api.get("/admin/leaderboard/config");
export const getAdminSettings = () => api.get("/admin/settings");

export const downloadAssessmentReport = (assessmentId, format = "csv") =>
  api.get(`/admin/reports/assessment/${assessmentId}`, {
    params: { format },
    responseType: "blob",
  });

export const downloadNonAttemptsReport = (assessmentId, format = "csv") =>
  api.get(`/admin/reports/non-attempts/${assessmentId}`, {
    params: { format },
    responseType: "blob",
  });
