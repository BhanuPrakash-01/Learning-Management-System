import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { jwtDecode } from "jwt-decode";
import { login as loginApi } from "../services/authService";

export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const res = await loginApi({ email, password });
      const { token } = res.data;

      if (!token) {
        setError(res.data.error || "Login failed");
        return;
      }

      localStorage.setItem("token", token);

      const decoded = jwtDecode(token);
      const role = decoded.role;

      navigate(role === "ADMIN" ? "/admin/dashboard" : "/student/home");
    } catch (err) {
      setError(err.response?.data?.error || "Login failed. Check your credentials.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-shell">
      <section className="auth-showcase">
        <div className="auth-brand">
          <div className="auth-brand-mark">SM</div>
          <span>Student Management System</span>
        </div>

        <div className="auth-copy">
          <h1>Learn, assess, and manage from one workspace.</h1>
          <p>
            The exported Figma design uses a clean teal identity, bright content surfaces,
            and a clear academic dashboard feel. This screen now mirrors that direction.
          </p>

          <div className="auth-metrics">
            <div className="auth-metric">
              <strong>24/7</strong>
              <span>Assessment access</span>
            </div>
            <div className="auth-metric">
              <strong>100%</strong>
              <span>Role-based workflows</span>
            </div>
            <div className="auth-metric">
              <strong>1 Hub</strong>
              <span>Courses, tests, results</span>
            </div>
          </div>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-card">
          <h2>Welcome back</h2>
          <p className="auth-subtitle">Sign in to continue to your dashboard.</p>

          {error && <div className="alert alert-error">{error}</div>}

          <form onSubmit={handleLogin} className="auth-actions">
            <div className="form-group">
              <label htmlFor="email">Email address</label>
              <input
                id="email"
                type="email"
                placeholder="student@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>

            <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
              {loading ? "Signing in..." : "Sign In"}
            </button>
          </form>

          <div className="demo-panel">
            <h4>Access notes</h4>
            <p>Students and admins use the same login. Your role determines the dashboard after sign in.</p>
          </div>

          <div className="auth-link">
            Don&apos;t have an account? <Link to="/register">Create one</Link>
          </div>
        </div>
      </section>
    </div>
  );
}
