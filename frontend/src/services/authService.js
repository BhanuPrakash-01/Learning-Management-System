import api from "../api/axios";

export const login = (data) => api.post("/auth/login", data);
export const register = (data) => api.post("/auth/register", data);
export const getMe = () => api.get("/auth/me");
export const logout = () => api.post("/auth/logout");
export const resendVerification = (data) => api.post("/auth/resend-verification", data);
export const forgotPassword = (data) => api.post("/auth/forgot-password", data);
export const resetPassword = (data) => api.post("/auth/reset-password", data);
export const verifyEmail = (token) => api.get(`/auth/verify-email?token=${encodeURIComponent(token)}`);
export const changePassword = (data) => api.post("/auth/change-password", data);
