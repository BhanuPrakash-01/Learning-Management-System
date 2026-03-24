import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import Layout from "../../components/Layout";
import { getAllAssessments } from "../../services/assessmentService";
import { getMyAttempts } from "../../services/attemptService";

const tabs = ["Upcoming", "Active", "Completed", "Missed"];

function getAssessmentState(assessment, attempts) {
  const now = new Date();
  const start = assessment.startTime ? new Date(assessment.startTime) : null;
  const end = assessment.endTime ? new Date(assessment.endTime) : null;
  const hasSubmitted = attempts.some(
    (attempt) => attempt.assessment?.id === assessment.id && attempt.submitted
  );

  if (hasSubmitted) return "Completed";
  if (end && now > end) return "Missed";
  if (start && end && now >= start && now <= end) return "Active";
  return "Upcoming";
}

export default function Assessments() {
  const [assessments, setAssessments] = useState([]);
  const [attempts, setAttempts] = useState([]);
  const [activeTab, setActiveTab] = useState("Active");
  const navigate = useNavigate();

  useEffect(() => {
    Promise.all([getAllAssessments(), getMyAttempts()])
      .then(([assessmentsRes, attemptsRes]) => {
        setAssessments(assessmentsRes.data || []);
        setAttempts(attemptsRes.data || []);
      })
      .catch((error) => console.error(error));
  }, []);

  const grouped = useMemo(() => {
    const initial = {
      Upcoming: [],
      Active: [],
      Completed: [],
      Missed: [],
    };
    assessments.forEach((assessment) => {
      const state = getAssessmentState(assessment, attempts);
      initial[state].push(assessment);
    });
    return initial;
  }, [assessments, attempts]);

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Assessments</div>
          <h1 className="page-title">Your targeted assessments only</h1>
          <p className="page-subtitle">
            Assessments are automatically filtered by your branch, batch, section, and active schedule window.
          </p>
        </div>
      </section>

      <section className="surface-panel">
        <div className="card-actions">
          {tabs.map((tab) => (
            <button
              key={tab}
              className={`btn ${activeTab === tab ? "btn-primary" : "btn-secondary"} btn-sm`}
              onClick={() => setActiveTab(tab)}
            >
              {tab} ({grouped[tab].length})
            </button>
          ))}
        </div>
      </section>

      {grouped[activeTab].length === 0 ? (
        <div className="empty-state">
          <p>No {activeTab.toLowerCase()} assessments right now.</p>
        </div>
      ) : (
        <div className="card-grid">
          {grouped[activeTab].map((assessment) => {
            const attemptsForAssessment = attempts.filter(
              (attempt) => attempt.assessment?.id === assessment.id
            );
            const completedAttempt = attemptsForAssessment.find((attempt) => attempt.submitted);

            return (
              <article key={assessment.id} className="card">
                <div className="list-card-top">
                  <div>
                    <h3>{assessment.title}</h3>
                    <p>{assessment.description || "No description available."}</p>
                  </div>
                  <span className="badge badge-primary">{assessment.assessmentType || "GENERAL"}</span>
                </div>

                <div className="card-meta">
                  <div className="meta-item">
                    <span>Duration</span>
                    <strong>{assessment.duration} minutes</strong>
                  </div>
                  <div className="meta-item">
                    <span>Start</span>
                    <strong>
                      {assessment.startTime ? new Date(assessment.startTime).toLocaleString() : "Always open"}
                    </strong>
                  </div>
                  <div className="meta-item">
                    <span>End</span>
                    <strong>
                      {assessment.endTime ? new Date(assessment.endTime).toLocaleString() : "No deadline"}
                    </strong>
                  </div>
                </div>

                <div className="card-actions">
                  {activeTab === "Completed" && completedAttempt && (
                    <>
                      <span className="badge badge-success">Score: {completedAttempt.score}</span>
                      <button
                        className="btn btn-secondary btn-sm"
                        onClick={() => navigate("/student/results")}
                      >
                        Review Answers
                      </button>
                    </>
                  )}
                  {activeTab === "Missed" && <span className="badge badge-danger">Closed - contact faculty</span>}
                  {(activeTab === "Active" || activeTab === "Upcoming") && (
                    <button className="btn btn-primary btn-sm" onClick={() => navigate(`/student/test/${assessment.id}`)}>
                      Start Assessment
                    </button>
                  )}
                </div>
              </article>
            );
          })}
        </div>
      )}
    </Layout>
  );
}
