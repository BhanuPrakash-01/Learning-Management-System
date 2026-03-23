import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import Layout from "../../components/Layout";
import { getStudentHome } from "../../services/homeService";

function formatDateTime(value) {
  if (!value) return "N/A";
  return new Date(value).toLocaleString();
}

export default function StudentHome() {
  const [data, setData] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    getStudentHome()
      .then((res) => setData(res.data))
      .catch((err) => console.error(err));
  }, []);

  const completion = useMemo(() => {
    if (!data) return 0;
    return Math.round(data.completionPercentage || 0);
  }, [data]);

  const progressColor = completion <= 30 ? "#64748b" : completion <= 70 ? "#d97706" : "#15803d";

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Student Home</div>
          <h1 className="page-title">Welcome back{data?.student?.name ? `, ${data.student.name}` : ""}</h1>
          <p className="page-subtitle">
            {data?.student
              ? `${data.student.rollNumber} · ${data.student.branch} · ${data.student.batchYear} · Section ${data.student.section}`
              : "Loading your personalized dashboard..."}
          </p>
        </div>

        <div className="hero-aside">
          <div className="hero-badge">
            <span className="hero-badge-label">Leaderboard Score</span>
            <span className="hero-badge-value">{data?.leaderboardScore ?? 0}</span>
          </div>
          <div className="hero-badge">
            <span className="hero-badge-label">Current Streak</span>
            <span className="hero-badge-value">{data?.currentStreak ?? 0} days</span>
          </div>
        </div>
      </section>

      <div className="stats-grid">
        <article className="stat-card">
          <div className="stat-label">Assigned Assessments</div>
          <div className="stat-value">{data?.assignedAssessments ?? 0}</div>
        </article>
        <article className="stat-card">
          <div className="stat-label">Completed</div>
          <div className="stat-value">{data?.completedAssessments ?? 0}</div>
        </article>
        <article className="stat-card interactive" onClick={() => navigate("/student/leaderboard")}>
          <div className="stat-label">Leaderboard</div>
          <div className="stat-value">Open</div>
        </article>
      </div>

      <section className="surface-panel">
        <div className="section-heading">
          <div>
            <h2 className="section-title">Semester Progress</h2>
            <p className="section-subtitle">{completion}% of assigned assessments completed.</p>
          </div>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: "1.5rem", marginTop: "1rem" }}>
          <div
            style={{
              width: "120px",
              height: "120px",
              borderRadius: "50%",
              border: `10px solid ${progressColor}`,
              display: "grid",
              placeItems: "center",
              fontWeight: 800,
              fontSize: "1.2rem",
              color: progressColor,
            }}
          >
            {completion}%
          </div>
          <div>
            <p>
              {data?.completedAssessments ?? 0} of {data?.assignedAssessments ?? 0} completed.
            </p>
            <p style={{ color: "var(--text-soft)" }}>Best streak: {data?.bestStreak ?? 0} days</p>
          </div>
        </div>
      </section>

      <section className="surface-panel">
        <div className="section-heading">
          <div>
            <h2 className="section-title">Upcoming Assessments</h2>
            <p className="section-subtitle">Next targeted assessments for your branch and batch.</p>
          </div>
          <button className="btn btn-secondary btn-sm" onClick={() => navigate("/student/assessments")}>
            View All
          </button>
        </div>

        {!data?.upcoming?.length ? (
          <div className="empty-state mt-2">
            <p>No upcoming assessments at the moment.</p>
          </div>
        ) : (
          <div className="card-grid mt-2">
            {data.upcoming.map((item) => (
              <article key={item.id} className="card">
                <h3>{item.title}</h3>
                <p>{item.description || "No description available."}</p>
                <div className="card-meta">
                  <div className="meta-item">
                    <span>Type</span>
                    <strong>{item.assessmentType || "GENERAL"}</strong>
                  </div>
                  <div className="meta-item">
                    <span>Starts</span>
                    <strong>{formatDateTime(item.startTime)}</strong>
                  </div>
                  <div className="meta-item">
                    <span>Duration</span>
                    <strong>{item.duration} min</strong>
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      <section className="surface-panel">
        <div className="section-heading">
          <div>
            <h2 className="section-title">Recent Activity</h2>
          </div>
        </div>

        {!data?.recentAttempts?.length ? (
          <div className="empty-state mt-2">
            <p>No recent assessment activity.</p>
          </div>
        ) : (
          <div className="card-grid mt-2">
            {data.recentAttempts.map((item, idx) => (
              <article key={`${item.assessment}-${idx}`} className="card">
                <h3>{item.assessment}</h3>
                <p>Score: {item.score}</p>
                <p style={{ color: "var(--text-muted)" }}>{formatDateTime(item.timestamp)}</p>
              </article>
            ))}
          </div>
        )}
      </section>
    </Layout>
  );
}
