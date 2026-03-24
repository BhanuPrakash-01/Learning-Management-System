import { useContext } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { AuthContext } from "../context/auth-context";

export default function ProtectedRoute({ children, requiredRole = null, allowWhenPasswordChange = false }) {
  const { user, loading } = useContext(AuthContext);
  const location = useLocation();

  if (loading) {
    return <div className="loading-skeleton" style={{ minHeight: "50vh" }} />;
  }

  if (!user) {
    return <Navigate to="/" replace />;
  }

  if (user.forcePasswordChange && !allowWhenPasswordChange) {
    return <Navigate to="/change-password" replace state={{ from: location.pathname }} />;
  }

  if (requiredRole && user.role !== requiredRole) {
    const fallback = user.role === "ADMIN" ? "/admin" : "/student";
    return <Navigate to={fallback} replace />;
  }

  return children;
}
