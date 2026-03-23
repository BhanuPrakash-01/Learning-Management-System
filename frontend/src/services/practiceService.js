import api from "../api/axios";

export const getPracticeCategories = () => api.get("/student/practice/categories");
export const getPracticeQuestions = (topicId, mode = "practice") =>
  api.get(`/student/practice/topics/${topicId}/questions`, { params: { mode } });
export const submitPracticeAttempt = (data) => api.post("/student/practice/attempt", data);
export const getPracticeProgress = () => api.get("/student/practice/progress");
