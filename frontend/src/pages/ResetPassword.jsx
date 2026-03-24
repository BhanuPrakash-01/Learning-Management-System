import { useState } from "react";
import { Link, useSearchParams, useNavigate } from "react-router-dom";
import { resetPassword } from "../services/authService";

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") || "";
  const navigate = useNavigate();

  const [form, setForm] = useState({ newPassword: "", confirmPassword: "" });
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setMessage("");
    if (!token) {
      setError("Missing or invalid reset token.");
      return;
    }
    setLoading(true);
    try {
      const res = await resetPassword({ token, ...form });
      setMessage(res.data?.message || "Password reset complete.");
      setTimeout(() => navigate("/"), 1200);
    } catch (err) {
      setError(err.response?.data?.error || "Unable to reset password.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-shell">
      <section className="auth-showcase">
        <div className="auth-brand">
          <div className="auth-brand-mark">AU</div>
          <span>Anurag University LMS</span>
        </div>
        <div className="auth-copy">
          <h1>Create a new password.</h1>
          <p>This reset token is valid for one hour and can only be used once.</p>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-card">
          <h2>Reset password</h2>
          <p className="auth-subtitle">Set a secure new password for your account.</p>

          {message && <div className="alert alert-success">{message}</div>}
          {error && <div className="alert alert-error">{error}</div>}

          <form onSubmit={handleSubmit} className="auth-actions">
            <div className="form-group">
              <label htmlFor="new-password">New password</label>
              <input
                id="new-password"
                type="password"
                value={form.newPassword}
                onChange={(e) => setForm((prev) => ({ ...prev, newPassword: e.target.value }))}
                required
              />
            </div>
            <div className="form-group">
              <label htmlFor="confirm-password">Confirm password</label>
              <input
                id="confirm-password"
                type="password"
                value={form.confirmPassword}
                onChange={(e) => setForm((prev) => ({ ...prev, confirmPassword: e.target.value }))}
                required
              />
            </div>
            <button className="btn btn-primary btn-block" disabled={loading}>
              {loading ? "Resetting..." : "Reset Password"}
            </button>
          </form>

          <div className="auth-link">
            <Link to="/">Back to sign in</Link>
          </div>
        </div>
      </section>
    </div>
  );
}
