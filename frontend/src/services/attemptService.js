import api from "../api/axios";

export const startAttempt = (assessmentId) =>
  api.post(`/student/attempts/start/${assessmentId}`);

export const saveAnswer = (attemptId, data) =>
  api.post(`/student/attempts/${attemptId}/answer`, data);

export const submitAttempt = (attemptId) =>
  api.post(`/student/attempts/${attemptId}/submit`);

export const getMyAttempts = () =>
  api.get("/student/attempts/my");

export const reviewAttempt = (attemptId) =>
  api.get(`/student/attempts/${attemptId}/review`);
