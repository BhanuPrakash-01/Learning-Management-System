import api from "../api/axios";

export const getAllAssessments = () => {
  return api.get("/student/assessments");
};
