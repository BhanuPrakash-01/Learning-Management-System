import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { register } from "../services/authService";

export default function Register() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleRegister = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);

    try {
      await register({ name, email, password });
      setSuccess("Registration successful. Redirecting to login...");
      setTimeout(() => navigate("/"), 1200);
    } catch (err) {
      setError(err.response?.data?.error || "Registration failed.");
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
          <h1>Create your student workspace.</h1>
          <p>
            Register once to access courses, participate in assessments, and track your
            progress in the redesigned academic portal.
          </p>

          <div className="auth-metrics">
            <div className="auth-metric">
              <strong>Fast</strong>
              <span>Simple onboarding</span>
            </div>
            <div className="auth-metric">
              <strong>Secure</strong>
              <span>Role-aware navigation</span>
            </div>
            <div className="auth-metric">
              <strong>Modern</strong>
              <span>Figma-inspired experience</span>
            </div>
          </div>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-card">
          <h2>Create account</h2>
          <p className="auth-subtitle">Register as a student to start learning.</p>

          {error && <div className="alert alert-error">{error}</div>}
          {success && <div className="alert alert-success">{success}</div>}

          <form onSubmit={handleRegister} className="auth-actions">
            <div className="form-group">
              <label htmlFor="name">Full name</label>
              <input
                id="name"
                placeholder="John Doe"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="register-email">Email address</label>
              <input
                id="register-email"
                type="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="register-password">Password</label>
              <input
                id="register-password"
                type="password"
                placeholder="Create a password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>

            <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
              {loading ? "Creating account..." : "Register"}
            </button>
          </form>

          <div className="auth-link">
            Already have an account? <Link to="/">Sign in</Link>
          </div>
        </div>
      </section>
    </div>
  );
}
