import { useEffect, useState } from "react";
import Layout from "../../components/Layout";
import { getMyAttempts } from "../../services/attemptService";

function getResultBadge(attempt) {
  if (!attempt.submitted) return ["badge badge-warning", "In Progress"];
  if (attempt.score >= 8) return ["badge badge-success", "Excellent"];
  if (attempt.score >= 5) return ["badge badge-primary", "Completed"];
  return ["badge badge-danger", "Needs Review"];
}

export default function MyResults() {
  const [attempts, setAttempts] = useState([]);

  useEffect(() => {
    loadAttempts();
  }, []);

  const loadAttempts = async () => {
    try {
      const res = await getMyAttempts();
      setAttempts(res.data);
    } catch (error) {
      console.error(error);
    }
  };

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">My results</div>
          <h1 className="page-title">Review your performance across submitted assessments.</h1>
          <p className="page-subtitle">
            Track scores, attempt history, and submission status in one place.
          </p>
        </div>
        <div className="hero-aside">
          <div className="hero-badge">
            <span className="hero-badge-label">Recorded attempts</span>
            <span className="hero-badge-value">{attempts.length}</span>
          </div>
        </div>
      </section>

      {attempts.length === 0 ? (
        <div className="empty-state">
          <p>No attempts yet. Take a test first.</p>
        </div>
      ) : (
        <div className="card-grid">
          {attempts.map((attempt) => {
            const [badgeClass, badgeLabel] = getResultBadge(attempt);

            return (
              <article key={attempt.id} className="card">
                <div className="list-card-top">
                  <div>
                    <h3>{attempt.assessment?.title || "Assessment"}</h3>
                    <p>{attempt.assessment?.course?.title || "Course unavailable"}</p>
                  </div>
                  <span className={badgeClass}>{badgeLabel}</span>
                </div>

                <div className="card-meta">
                  <div className="meta-item">
                    <span>Score</span>
                    <strong>{attempt.score ?? 0}</strong>
                  </div>
                  <div className="meta-item">
                    <span>Started</span>
                    <strong>{attempt.startTime ? new Date(attempt.startTime).toLocaleString() : "N/A"}</strong>
                  </div>
                  <div className="meta-item">
                    <span>Finished</span>
                    <strong>{attempt.endTime ? new Date(attempt.endTime).toLocaleString() : "N/A"}</strong>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </Layout>
  );
}
