import api from "../api/axios";

export const getStudentNotifications = (params = {}) =>
  api.get("/student/notifications", { params });

export const markNotificationRead = (id) =>
  api.patch(`/student/notifications/${id}/read`);
