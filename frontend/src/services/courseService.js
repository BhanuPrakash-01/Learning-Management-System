import api from "../api/axios";

export const getCourses = () => {
  return api.get("/student/courses");
};

export const enrollCourse = (courseId) => {
  return api.post(`/student/enrollments/${courseId}`);
};