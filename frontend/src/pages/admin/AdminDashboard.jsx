import { useEffect, useState } from "react";
import Layout from "../../components/Layout";
import { getDashboardStats } from "../../services/adminService";

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);

  useEffect(() => {
    getDashboardStats()
      .then((res) => setStats(res.data))
      .catch((err) => console.error(err));
  }, []);

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Admin Dashboard</div>
          <h1 className="page-title">College-wide LMS analytics</h1>
          <p className="page-subtitle">
            Track enrollment distribution, assessment activity, and intervention candidates.
          </p>
        </div>
      </section>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-label">Total Students</div>
          <div className="stat-value">{stats?.totalStudents ?? 0}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Active Assessments</div>
          <div className="stat-value">{stats?.activeAssessments ?? 0}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Average Score</div>
          <div className="stat-value">{Math.round((stats?.avgScore ?? 0) * 100) / 100}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Students with 0 Attempts</div>
          <div className="stat-value">{stats?.studentsWithZeroAttempts ?? 0}</div>
        </div>
      </div>

      <section className="surface-panel">
        <div className="section-heading">
          <div>
            <h2 className="section-title">Branch Breakdown</h2>
          </div>
        </div>
        <div className="card-grid mt-2">
          {Object.entries(stats?.branchBreakdown || {}).map(([branch, count]) => (
            <article key={branch} className="card">
              <h3>{branch}</h3>
              <p>{count} students</p>
            </article>
          ))}
        </div>
      </section>

      <section className="surface-panel">
        <div className="section-heading">
          <div>
            <h2 className="section-title">Top Performers</h2>
          </div>
        </div>
        <div className="card-grid mt-2">
          {(stats?.topPerformers || []).map((item, index) => (
            <article key={`${item.rollNumber}-${index}`} className="card">
              <h3>{item.name}</h3>
              <p>{item.rollNumber}</p>
              <p>{item.branch}</p>
              <p style={{ fontWeight: 700 }}>Score: {item.totalScore}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="surface-panel">
        <div className="section-heading">
          <div>
            <h2 className="section-title">Recent Assessment Activity</h2>
          </div>
        </div>
        <div className="card-grid mt-2">
          {(stats?.recentActivity || []).map((item, index) => (
            <article key={`${item.rollNumber}-${index}`} className="card">
              <h3>{item.assessment}</h3>
              <p>{item.student} ({item.rollNumber})</p>
              <p>Score: {item.score}</p>
              <p style={{ color: "var(--text-muted)" }}>
                {item.timestamp ? new Date(item.timestamp).toLocaleString() : "N/A"}
              </p>
            </article>
          ))}
        </div>
      </section>
    </Layout>
  );
}
