import axios from "axios";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8000/api";

const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
});

const refreshClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (!originalRequest) {
      return Promise.reject(error);
    }

    const status = error.response?.status;
    const path = originalRequest.url || "";
    const authExclusions = ["/auth/login", "/auth/register", "/auth/refresh"];
    const isExcluded = authExclusions.some((item) => path.includes(item));

    if (status === 401 && !originalRequest._retry && !isExcluded) {
      originalRequest._retry = true;
      try {
        await refreshClient.post("/auth/refresh");
        return api(originalRequest);
      } catch (refreshError) {
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
