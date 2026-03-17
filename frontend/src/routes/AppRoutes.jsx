import { Routes, Route } from "react-router-dom";
import ProtectedRoute from "./ProtectedRoute";

import Login from "../pages/Login";
import Register from "../pages/Register";
import StudentDashboard from "../pages/student/StudentDashboard";
import AdminDashboard from "../pages/admin/AdminDashboard";
import Assessments from "../pages/student/Assessments";
import TakeTest from "../pages/student/TakeTest";
import MyResults from "../pages/student/MyResults";
import Courses from "../pages/admin/Courses";
import AdminAssessments from "../pages/admin/Assessments";
import Questions from "../pages/admin/Questions";

export default function AppRoutes() {
  return (
    <Routes>
      {/* Auth */}
      <Route path="/" element={<Login />} />
      <Route path="/register" element={<Register />} />

      {/* Student */}
      <Route path="/student/dashboard" element={<ProtectedRoute><StudentDashboard /></ProtectedRoute>} />
      <Route path="/student/assessments" element={<ProtectedRoute><Assessments /></ProtectedRoute>} />
      <Route path="/student/test/:assessmentId" element={<ProtectedRoute><TakeTest /></ProtectedRoute>} />
      <Route path="/student/results" element={<ProtectedRoute><MyResults /></ProtectedRoute>} />

      {/* Admin */}
      <Route path="/admin/dashboard" element={<ProtectedRoute><AdminDashboard /></ProtectedRoute>} />
      <Route path="/admin/courses" element={<ProtectedRoute><Courses /></ProtectedRoute>} />
      <Route path="/admin/assessments" element={<ProtectedRoute><AdminAssessments /></ProtectedRoute>} />
      <Route path="/admin/questions" element={<ProtectedRoute><Questions /></ProtectedRoute>} />
    </Routes>
  );
}