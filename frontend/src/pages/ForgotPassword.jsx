import { useState } from "react";
import { Link } from "react-router-dom";
import { forgotPassword } from "../services/authService";

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMessage("");
    setError("");
    try {
      const res = await forgotPassword({ email });
      setMessage(res.data?.message || "Reset link sent if the account exists.");
    } catch (err) {
      setError(err.response?.data?.error || "Unable to process request.");
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
          <h1>Reset your password securely.</h1>
          <p>Enter your registered email and we will send a one-time reset link that expires in 1 hour.</p>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-card">
          <h2>Forgot password</h2>
          <p className="auth-subtitle">Receive a secure reset link in your inbox.</p>

          {message && <div className="alert alert-success">{message}</div>}
          {error && <div className="alert alert-error">{error}</div>}

          <form onSubmit={handleSubmit} className="auth-actions">
            <div className="form-group">
              <label htmlFor="forgot-email">Email</label>
              <input
                id="forgot-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            <button className="btn btn-primary btn-block" disabled={loading}>
              {loading ? "Sending..." : "Send Reset Link"}
            </button>
          </form>

          <div className="auth-link">
            Remembered your password? <Link to="/">Back to sign in</Link>
          </div>
        </div>
      </section>
    </div>
  );
}
