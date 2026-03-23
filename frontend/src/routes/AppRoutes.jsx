import { Routes, Route } from "react-router-dom";
import ProtectedRoute from "./ProtectedRoute";

import Login from "../pages/Login";
import Register from "../pages/Register";

import StudentHome from "../pages/student/Home";
import Assessments from "../pages/student/Assessments";
import TakeTest from "../pages/student/TakeTest";
import MyResults from "../pages/student/MyResults";
import Practice from "../pages/student/Practice";
import CodingLab from "../pages/student/CodingLab";
import Leaderboard from "../pages/student/Leaderboard";
import Profile from "../pages/student/Profile";

import AdminDashboard from "../pages/admin/AdminDashboard";
import Students from "../pages/admin/Students";
import ContentHub from "../pages/admin/ContentHub";
import PracticeManager from "../pages/admin/PracticeManager";
import LeaderboardConfig from "../pages/admin/LeaderboardConfig";
import Reports from "../pages/admin/Reports";
import Settings from "../pages/admin/Settings";

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Login />} />
      <Route path="/register" element={<Register />} />

      <Route path="/student/home" element={<ProtectedRoute><StudentHome /></ProtectedRoute>} />
      <Route path="/student/assessments" element={<ProtectedRoute><Assessments /></ProtectedRoute>} />
      <Route path="/student/test/:assessmentId" element={<ProtectedRoute><TakeTest /></ProtectedRoute>} />
      <Route path="/student/practice" element={<ProtectedRoute><Practice /></ProtectedRoute>} />
      <Route path="/student/coding" element={<ProtectedRoute><CodingLab /></ProtectedRoute>} />
      <Route path="/student/leaderboard" element={<ProtectedRoute><Leaderboard /></ProtectedRoute>} />
      <Route path="/student/results" element={<ProtectedRoute><MyResults /></ProtectedRoute>} />
      <Route path="/student/profile" element={<ProtectedRoute><Profile /></ProtectedRoute>} />

      <Route path="/admin/dashboard" element={<ProtectedRoute><AdminDashboard /></ProtectedRoute>} />
      <Route path="/admin/students" element={<ProtectedRoute><Students /></ProtectedRoute>} />
      <Route path="/admin/content" element={<ProtectedRoute><ContentHub /></ProtectedRoute>} />
      <Route path="/admin/practice" element={<ProtectedRoute><PracticeManager /></ProtectedRoute>} />
      <Route path="/admin/leaderboard" element={<ProtectedRoute><LeaderboardConfig /></ProtectedRoute>} />
      <Route path="/admin/reports" element={<ProtectedRoute><Reports /></ProtectedRoute>} />
      <Route path="/admin/settings" element={<ProtectedRoute><Settings /></ProtectedRoute>} />
    </Routes>
  );
}
