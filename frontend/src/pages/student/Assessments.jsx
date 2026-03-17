import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Layout from "../../components/Layout";
import { getAllAssessments } from "../../services/assessmentService";

export default function Assessments() {
  const [assessments, setAssessments] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    loadAssessments();
  }, []);

  const loadAssessments = async () => {
    try {
      const res = await getAllAssessments();
      setAssessments(res.data);
    } catch (error) {
      console.error(error);
    }
  };

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Assessment center</div>
          <h1 className="page-title">Start the right assessment at the right time.</h1>
          <p className="page-subtitle">
            Review durations, linked courses, and descriptions before launching the test flow.
          </p>
        </div>

        <div className="hero-aside">
          <div className="hero-badge">
            <span className="hero-badge-label">Available assessments</span>
            <span className="hero-badge-value">{assessments.length}</span>
          </div>
        </div>
      </section>

      {assessments.length === 0 ? (
        <div className="empty-state">
          <p>No assessments available yet.</p>
        </div>
      ) : (
        <div className="card-grid">
          {assessments.map((assessment) => (
            <article key={assessment.id} className="card">
              <div className="list-card-top">
                <div>
                  <h3>{assessment.title}</h3>
                  <p>{assessment.description || "No description available."}</p>
                </div>
                <span className="badge badge-primary">{assessment.course?.title || "General"}</span>
              </div>

              <div className="card-meta">
                <div className="meta-item">
                  <span>Duration</span>
                  <strong>{assessment.duration} minutes</strong>
                </div>
              </div>

              <div className="card-actions">
                <button className="btn btn-primary" onClick={() => navigate(`/student/test/${assessment.id}`)}>
                  Start Assessment
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </Layout>
  );
}
