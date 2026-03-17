import { NavLink, useNavigate } from "react-router-dom";
import { jwtDecode } from "jwt-decode";

function getUserFromToken(token) {
  if (!token) return null;

  try {
    const decoded = jwtDecode(token);
    return {
      role: decoded.role || null,
      name: decoded.name || decoded.sub || "User",
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
          { path: "/admin/dashboard", label: "Analytics" },
          { path: "/admin/courses", label: "Courses" },
          { path: "/admin/assessments", label: "Assessments" },
          { path: "/admin/questions", label: "Questions" },
        ]
      : [
          { path: "/student/dashboard", label: "Dashboard" },
          { path: "/student/assessments", label: "Assessments" },
          { path: "/student/results", label: "Results" },
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
          <div className="nav-brand-mark">SM</div>
          <div className="nav-brand-copy">
            <span className="nav-brand-title">Student Management</span>
            <span className="nav-brand-subtitle">
              {user?.role === "ADMIN" ? "Administration workspace" : "Learning workspace"}
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
              <div className="nav-user-role">{user.role}</div>
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
