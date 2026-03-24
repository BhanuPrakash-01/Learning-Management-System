import { useContext, useMemo, useState } from "react";
import { NavLink, Link, useNavigate } from "react-router-dom";
import { AuthContext } from "../context/auth-context";

export default function Navbar() {
  const auth = useContext(AuthContext) || {};
  const user = auth.user || null;
  const logout = auth.logout || (() => Promise.resolve());
  const navigate = useNavigate();
  const [openMenu, setOpenMenu] = useState(false);

  const links = useMemo(() => {
    if (user?.role === "ADMIN") {
      return [
        { path: "/admin/assessments", label: "Assessments" },
        { path: "/admin/students", label: "Students" },
        { path: "/admin/practice", label: "Practice" },
        { path: "/admin/reports", label: "Reports" },
      ];
    }
    return [
      { path: "/student", label: "Home" },
      { path: "/student/assessments", label: "Assessments" },
      { path: "/student/practice", label: "Practice" },
      { path: "/student/coding", label: "Coding Lab" },
      { path: "/student/leaderboard", label: "Leaderboard" },
    ];
  }, [user?.role]);

  const initials = (user?.name || "U")
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");

  const handleLogout = () => {
    logout().finally(() => navigate("/"));
  };

  const menuLinks =
    user?.role === "ADMIN"
      ? [
          { to: "/admin/profile", label: "Profile" },
          { to: "/change-password", label: "Change Password" },
        ]
      : [
          { to: "/student/profile", label: "My Profile" },
          { to: "/student/results", label: "My Results" },
          { to: "/change-password", label: "Change Password" },
        ];

  return (
    <header className="navbar">
      <div className="nav-inner">
        <div className="nav-brand">
          <div className="nav-brand-mark">AU</div>
          <div className="nav-brand-copy">
            <Link to={user?.role === "ADMIN" ? "/admin" : "/student"} className="nav-brand-title">
              Anurag University LMS
            </Link>
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
            <button className="nav-avatar" onClick={() => setOpenMenu((prev) => !prev)}>{initials}</button>
            {openMenu && (
              <div className="nav-dropdown">
                {menuLinks.map((item) => (
                  <Link key={item.to} to={item.to} className="nav-dropdown-link" onClick={() => setOpenMenu(false)}>
                    {item.label}
                  </Link>
                ))}
                <button className="nav-dropdown-link danger" onClick={handleLogout}>
                  Logout
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </header>
  );
}
