import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { verifyEmail } from "../services/authService";

export default function VerifyEmail() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") || "";
  const [status, setStatus] = useState("loading");
  const [message, setMessage] = useState("Verifying your email...");

  const missingToken = !token;

  useEffect(() => {
    if (missingToken) return;
    verifyEmail(token)
      .then((res) => {
        setStatus("success");
        setMessage(res.data?.message || "Email verified successfully.");
      })
      .catch((err) => {
        setStatus("error");
        setMessage(err.response?.data?.error || "Verification failed.");
      });
  }, [token, missingToken]);

  if (missingToken) {
    return (
      <div className="auth-shell">
        <section className="auth-showcase">
          <div className="auth-brand">
            <div className="auth-brand-mark">AU</div>
            <span>Anurag University LMS</span>
          </div>
          <div className="auth-copy">
            <h1>Account verification</h1>
            <p>Missing verification token.</p>
          </div>
        </section>
        <section className="auth-panel">
          <div className="auth-card">
            <div className="alert alert-error">Missing verification token.</div>
            <div className="auth-link">
              <Link to="/">Go to sign in</Link>
            </div>
          </div>
        </section>
      </div>
    );
  }

  return (
    <div className="auth-shell">
      <section className="auth-showcase">
        <div className="auth-brand">
          <div className="auth-brand-mark">AU</div>
          <span>Anurag University LMS</span>
        </div>
        <div className="auth-copy">
          <h1>Account verification</h1>
          <p>We are validating your email token.</p>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-card">
          <h2>Email verification</h2>
          <div className={`alert ${status === "success" ? "alert-success" : status === "error" ? "alert-error" : ""}`}>
            {message}
          </div>
          <div className="auth-link">
            <Link to="/">Go to sign in</Link>
          </div>
        </div>
      </section>
    </div>
  );
}
