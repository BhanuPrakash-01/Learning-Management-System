import { useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { register } from "../services/authService";

const branches = ["CSE", "CSE-AI", "ECE", "EEE", "MECH", "CIVIL", "IT", "DS"];
const sections = ["A", "B", "C", "D", "E"];
const batchYears = [2022, 2023, 2024, 2025, 2026, 2027];
const rollRegex = /^[0-9]{2}EG1[0-9]{2}[A-E][0-9]{2}$/;

export default function Register() {
  const [step, setStep] = useState(1);
  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    phone: "",
    rollNumber: "",
    branch: "",
    batchYear: "",
    section: "",
  });
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const rollValid = useMemo(() => rollRegex.test(form.rollNumber.toUpperCase()), [form.rollNumber]);
  const emailValid = useMemo(() => form.email.toLowerCase().endsWith("@anurag.ac.in"), [form.email]);
  const phoneValid = useMemo(
    () => !form.phone || /^[0-9]{10}$/.test(form.phone),
    [form.phone]
  );

  const updateField = (key, value) => setForm((prev) => ({ ...prev, [key]: value }));

  const nextStep = () => {
    if (!form.name || !form.email || !form.password) {
      setError("Please complete all required personal details.");
      return;
    }
    if (!emailValid) {
      setError("Use your @anurag.ac.in email address.");
      return;
    }
    if (form.password.length < 8) {
      setError("Password must be at least 8 characters.");
      return;
    }
    if (!phoneValid) {
      setError("Phone number must be 10 digits.");
      return;
    }
    setError("");
    setStep(2);
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");

    if (!rollValid) {
      setError("Roll number must match 23EG106D01 format.");
      return;
    }
    if (!form.branch || !form.batchYear || !form.section) {
      setError("Please complete all academic details.");
      return;
    }

    setLoading(true);
    try {
      await register({
        ...form,
        email: form.email.toLowerCase(),
        rollNumber: form.rollNumber.toUpperCase(),
        section: form.section.toUpperCase(),
        batchYear: Number(form.batchYear),
      });
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
          <div className="auth-brand-mark">AU</div>
          <span>Anurag University LMS</span>
        </div>

        <div className="auth-copy">
          <h1>Register your academic identity once.</h1>
          <p>
            Your branch, batch, section, and roll number are used to auto-personalize
            assessments, practice, and leaderboard visibility.
          </p>
          <div className="auth-metrics">
            <div className="auth-metric">
              <strong>Step {step}/2</strong>
              <span>{step === 1 ? "Personal details" : "Academic details"}</span>
            </div>
            <div className="auth-metric">
              <strong>Secure</strong>
              <span>@anurag.ac.in only</span>
            </div>
            <div className="auth-metric">
              <strong>Smart</strong>
              <span>Automatic content targeting</span>
            </div>
          </div>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-card">
          <h2>Create account</h2>
          <p className="auth-subtitle">
            {step === 1 ? "Step 1: Personal details" : "Step 2: Academic details"}
          </p>

          {error && <div className="alert alert-error">{error}</div>}
          {success && <div className="alert alert-success">{success}</div>}

          <form onSubmit={handleRegister} className="auth-actions">
            {step === 1 && (
              <>
                <div className="form-group">
                  <label htmlFor="name">Full name</label>
                  <input
                    id="name"
                    placeholder="John Doe"
                    value={form.name}
                    onChange={(e) => updateField("name", e.target.value)}
                    required
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="register-email">Email address</label>
                  <input
                    id="register-email"
                    type="email"
                    placeholder="you@anurag.ac.in"
                    value={form.email}
                    onChange={(e) => updateField("email", e.target.value)}
                    required
                  />
                  <small className="helper-text">Use your @anurag.ac.in email address.</small>
                </div>

                <div className="form-group">
                  <label htmlFor="register-password">Password</label>
                  <input
                    id="register-password"
                    type="password"
                    placeholder="Minimum 8 characters"
                    value={form.password}
                    onChange={(e) => updateField("password", e.target.value)}
                    required
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="phone">Phone number (optional)</label>
                  <input
                    id="phone"
                    placeholder="10-digit mobile number"
                    value={form.phone}
                    onChange={(e) => updateField("phone", e.target.value)}
                  />
                </div>

                <button type="button" className="btn btn-primary btn-block" onClick={nextStep}>
                  Continue to Academic Details
                </button>
              </>
            )}

            {step === 2 && (
              <>
                <div className="form-group">
                  <label htmlFor="roll">Roll number</label>
                  <input
                    id="roll"
                    placeholder="23EG106D01"
                    value={form.rollNumber}
                    onChange={(e) => updateField("rollNumber", e.target.value.toUpperCase())}
                    required
                  />
                  <small className="helper-text" style={{ color: rollValid ? "var(--success)" : "var(--danger)" }}>
                    {rollValid ? "Valid roll number format." : "Expected format: 23EG106D01 (YYEG1SSXRR)"}
                  </small>
                </div>

                <div className="form-group">
                  <label htmlFor="branch">Branch</label>
                  <select
                    id="branch"
                    value={form.branch}
                    onChange={(e) => updateField("branch", e.target.value)}
                    required
                  >
                    <option value="">Select branch</option>
                    {branches.map((branch) => (
                      <option key={branch} value={branch}>
                        {branch}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-group">
                  <label htmlFor="batchYear">Batch year</label>
                  <select
                    id="batchYear"
                    value={form.batchYear}
                    onChange={(e) => updateField("batchYear", e.target.value)}
                    required
                  >
                    <option value="">Select batch year</option>
                    {batchYears.map((year) => (
                      <option key={year} value={year}>
                        {year}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-group">
                  <label htmlFor="section">Section</label>
                  <select
                    id="section"
                    value={form.section}
                    onChange={(e) => updateField("section", e.target.value)}
                    required
                  >
                    <option value="">Select section</option>
                    {sections.map((section) => (
                      <option key={section} value={section}>
                        {section}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="card-actions">
                  <button type="button" className="btn btn-secondary" onClick={() => setStep(1)}>
                    Back
                  </button>
                  <button type="submit" className="btn btn-primary" disabled={loading}>
                    {loading ? "Creating account..." : "Register"}
                  </button>
                </div>
              </>
            )}
          </form>

          <div className="auth-link">
            Already have an account? <Link to="/">Sign in</Link>
          </div>
        </div>
      </section>
    </div>
  );
}
