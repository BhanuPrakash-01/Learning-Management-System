import api from "../api/axios";

export const getAllAssessments = () => {
  return api.get("/student/assessments");
};

export const getAssessmentsByCourse = (courseId) => {
  return api.get(`/student/assessments/course/${courseId}`);
};