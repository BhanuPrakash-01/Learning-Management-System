import { NavLink, useNavigate } from "react-router-dom";
import { jwtDecode } from "jwt-decode";

function getUserFromToken(token) {
  if (!token) return null;
  try {
    const decoded = jwtDecode(token);
    return {
      role: decoded.role || null,
      name: decoded.name || decoded.sub || "User",
      rollNumber: decoded.rollNumber || "",
      branch: decoded.branch || "",
      batchYear: decoded.batchYear || "",
      section: decoded.section || "",
    };
  } catch {
    localStorage.removeItem("token");
    return null;
  }
}

export default function Navbar() {
  const navigate = useNavigate();
  const token = localStorage.getItem("token");
  const user = getUserFromToken(token);

  const links =
    user?.role === "ADMIN"
      ? [
          { path: "/admin/dashboard", label: "Dashboard" },
          { path: "/admin/students", label: "Students" },
          { path: "/admin/content", label: "Content Hub" },
          { path: "/admin/practice", label: "Practice" },
          { path: "/admin/leaderboard", label: "Leaderboard Config" },
          { path: "/admin/reports", label: "Reports" },
          { path: "/admin/settings", label: "Settings" },
        ]
      : [
          { path: "/student/home", label: "Home" },
          { path: "/student/assessments", label: "Assessments" },
          { path: "/student/practice", label: "Practice" },
          { path: "/student/coding", label: "Coding Lab" },
          { path: "/student/leaderboard", label: "Leaderboard" },
          { path: "/student/results", label: "My Results" },
          { path: "/student/profile", label: "My Profile" },
        ];

  const initials = (user?.name || "U")
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");

  const handleLogout = () => {
    localStorage.removeItem("token");
    navigate("/");
  };

  return (
    <header className="navbar">
      <div className="nav-inner">
        <div className="nav-brand">
          <div className="nav-brand-mark">AU</div>
          <div className="nav-brand-copy">
            <span className="nav-brand-title">Anurag University LMS</span>
            <span className="nav-brand-subtitle">
              {user?.role === "ADMIN" ? "Administration workspace" : "Student workspace"}
            </span>
          </div>
        </div>

        <nav className="nav-links">
          {links.map((link) => (
            <NavLink
              key={link.path}
              to={link.path}
              className={({ isActive }) => `nav-link${isActive ? " active" : ""}`}
            >
              {link.label}
            </NavLink>
          ))}
        </nav>

        {user && (
          <div className="nav-user">
            <div className="nav-user-meta">
              <div className="nav-user-name">{user.name}</div>
              <div className="nav-user-role">
                {user.role}
                {user.role === "STUDENT" && user.rollNumber ? ` · ${user.rollNumber}` : ""}
              </div>
            </div>
            <div className="nav-avatar">{initials}</div>
            <button className="btn btn-danger btn-sm" onClick={handleLogout}>
              Logout
            </button>
          </div>
        )}
      </div>
    </header>
  );
}
