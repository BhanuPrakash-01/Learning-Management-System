import { Routes, Route } from "react-router-dom";
import ProtectedRoute from "./ProtectedRoute";

import Login from "../pages/Login";
import Register from "../pages/Register";
import ForgotPassword from "../pages/ForgotPassword";
import ResetPassword from "../pages/ResetPassword";
import VerifyEmail from "../pages/VerifyEmail";
import ChangePassword from "../pages/ChangePassword";

import StudentHome from "../pages/student/Home";
import Assessments from "../pages/student/Assessments";
import TakeTest from "../pages/student/TakeTest";
import MyResults from "../pages/student/MyResults";
import Practice from "../pages/student/Practice";
import CodingLab from "../pages/student/CodingLab";
import Leaderboard from "../pages/student/Leaderboard";
import Profile from "../pages/student/Profile";

import AdminDashboard from "../pages/admin/AdminDashboard";
import AdminProfile from "../pages/admin/AdminProfile";
import Students from "../pages/admin/Students";
import ContentHub from "../pages/admin/ContentHub";
import PracticeManager from "../pages/admin/PracticeManager";
import Reports from "../pages/admin/Reports";

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route path="/verify-email" element={<VerifyEmail />} />

      <Route path="/change-password" element={<ProtectedRoute allowWhenPasswordChange><ChangePassword /></ProtectedRoute>} />

      <Route path="/student" element={<ProtectedRoute requiredRole="STUDENT"><StudentHome /></ProtectedRoute>} />
      <Route path="/student/home" element={<ProtectedRoute requiredRole="STUDENT"><StudentHome /></ProtectedRoute>} />
      <Route path="/student/assessments" element={<ProtectedRoute requiredRole="STUDENT"><Assessments /></ProtectedRoute>} />
      <Route path="/student/test/:assessmentId" element={<ProtectedRoute requiredRole="STUDENT"><TakeTest /></ProtectedRoute>} />
      <Route path="/student/practice" element={<ProtectedRoute requiredRole="STUDENT"><Practice /></ProtectedRoute>} />
      <Route path="/student/coding" element={<ProtectedRoute requiredRole="STUDENT"><CodingLab /></ProtectedRoute>} />
      <Route path="/student/leaderboard" element={<ProtectedRoute requiredRole="STUDENT"><Leaderboard /></ProtectedRoute>} />
      <Route path="/student/results" element={<ProtectedRoute requiredRole="STUDENT"><MyResults /></ProtectedRoute>} />
      <Route path="/student/profile" element={<ProtectedRoute requiredRole="STUDENT"><Profile /></ProtectedRoute>} />

      <Route path="/admin" element={<ProtectedRoute requiredRole="ADMIN"><AdminDashboard /></ProtectedRoute>} />
      <Route path="/admin/dashboard" element={<ProtectedRoute requiredRole="ADMIN"><AdminDashboard /></ProtectedRoute>} />
      <Route path="/admin/profile" element={<ProtectedRoute requiredRole="ADMIN"><AdminProfile /></ProtectedRoute>} />
      <Route path="/admin/students" element={<ProtectedRoute requiredRole="ADMIN"><Students /></ProtectedRoute>} />
      <Route path="/admin/assessments" element={<ProtectedRoute requiredRole="ADMIN"><ContentHub /></ProtectedRoute>} />
      <Route path="/admin/content" element={<ProtectedRoute requiredRole="ADMIN"><ContentHub /></ProtectedRoute>} />
      <Route path="/admin/practice" element={<ProtectedRoute requiredRole="ADMIN"><PracticeManager /></ProtectedRoute>} />
      <Route path="/admin/reports" element={<ProtectedRoute requiredRole="ADMIN"><Reports /></ProtectedRoute>} />
    </Routes>
  );
}
