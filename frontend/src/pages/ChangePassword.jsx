import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Layout from "../components/Layout";
import { changePassword } from "../services/authService";
import { useContext } from "react";
import { AuthContext } from "../context/auth-context";

export default function ChangePassword() {
  const navigate = useNavigate();
  const { refreshUser, user } = useContext(AuthContext);
  const [form, setForm] = useState({ currentPassword: "", newPassword: "", confirmPassword: "" });
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setMessage("");
    setLoading(true);

    try {
      const res = await changePassword(form);
      setMessage(res.data?.message || "Password changed.");
      await refreshUser();
      setTimeout(() => {
        if (user?.role === "ADMIN") {
          navigate("/admin");
        } else {
          navigate("/student");
        }
      }, 900);
    } catch (err) {
      setError(err.response?.data?.error || "Unable to change password.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Security</div>
          <h1 className="page-title">Change your password</h1>
          <p className="page-subtitle">Use a strong password with at least 8 characters.</p>
        </div>
      </section>

      <section className="surface-panel">
        {message && <div className="alert alert-success">{message}</div>}
        {error && <div className="alert alert-error">{error}</div>}

        <form onSubmit={handleSubmit} className="form-grid">
          <div className="form-group">
            <label htmlFor="current-password">Current password</label>
            <input
              id="current-password"
              type="password"
              value={form.currentPassword}
              onChange={(e) => setForm((prev) => ({ ...prev, currentPassword: e.target.value }))}
              required
            />
          </div>
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
            <label htmlFor="confirm-new-password">Confirm new password</label>
            <input
              id="confirm-new-password"
              type="password"
              value={form.confirmPassword}
              onChange={(e) => setForm((prev) => ({ ...prev, confirmPassword: e.target.value }))}
              required
            />
          </div>
          <div className="card-actions form-group-full">
            <button className="btn btn-primary" disabled={loading}>
              {loading ? "Saving..." : "Update Password"}
            </button>
          </div>
        </form>
      </section>
    </Layout>
  );
}
