import { useEffect, useState } from "react";
import Layout from "../../components/Layout";
import { getLeaderboard } from "../../services/leaderboardService";

const scopes = [
  { key: "global", label: "Global" },
  { key: "branch", label: "My Branch" },
  { key: "batch", label: "My Batch" },
  { key: "section", label: "My Section" },
];

export default function Leaderboard() {
  const [scope, setScope] = useState("global");
  const [rows, setRows] = useState([]);

  useEffect(() => {
    getLeaderboard({ scope })
      .then((res) => setRows(res.data || []))
      .catch((err) => console.error(err));
  }, [scope]);

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Leaderboard</div>
          <h1 className="page-title">Track your rank across cohorts</h1>
          <p className="page-subtitle">
            View rankings by global, branch, batch, and section scopes.
          </p>
        </div>
      </section>

      <section className="surface-panel">
        <div className="card-actions">
          {scopes.map((item) => (
            <button
              key={item.key}
              className={`btn ${scope === item.key ? "btn-primary" : "btn-secondary"} btn-sm`}
              onClick={() => setScope(item.key)}
            >
              {item.label}
            </button>
          ))}
        </div>
      </section>

      <section className="surface-panel">
        {!rows.length ? (
          <div className="empty-state">
            <p>No leaderboard data yet.</p>
          </div>
        ) : (
          <div style={{ overflowX: "auto" }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Rank</th>
                  <th>Name</th>
                  <th>Roll No</th>
                  <th>Branch</th>
                  <th>Total</th>
                  <th>Assessment</th>
                  <th>Practice</th>
                  <th>Coding</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.studentId} className={row.isCurrentUser ? "highlight-row" : ""}>
                    <td>{row.rank}</td>
                    <td>{row.name}</td>
                    <td>{row.rollNumber}</td>
                    <td>{row.branch}</td>
                    <td>{row.totalScore}</td>
                    <td>{row.assessmentScore}</td>
                    <td>{row.practiceScore}</td>
                    <td>{row.codingScore}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </Layout>
  );
}
