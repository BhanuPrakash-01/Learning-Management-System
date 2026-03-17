import api from "../api/axios";

export const getQuestionsByAssessment = (assessmentId) =>
  api.get(`/student/questions/${assessmentId}`);
