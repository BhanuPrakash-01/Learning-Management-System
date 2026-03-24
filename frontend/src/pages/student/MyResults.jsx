import { useEffect, useState } from "react";
import Layout from "../../components/Layout";
import { reviewAttempt } from "../../services/attemptService";
import { getStudentResults } from "../../services/resultsService";

export default function MyResults() {
  const [data, setData] = useState(null);
  const [reviewData, setReviewData] = useState(null);
  const [reviewError, setReviewError] = useState("");

  useEffect(() => {
    getStudentResults()
      .then((res) => setData(res.data))
      .catch((err) => console.error(err));
  }, []);

  const openReview = async (attemptId) => {
    setReviewError("");
    try {
      const res = await reviewAttempt(attemptId);
      setReviewData(res.data);
    } catch (err) {
      setReviewError(err.response?.data?.error || "Failed to load review.");
    }
  };

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">My Results</div>
          <h1 className="page-title">Assessment and practice performance</h1>
          <p className="page-subtitle">Track attempts, score trends, and detailed answer reviews.</p>
        </div>
      </section>

      <section className="surface-panel">
        <div className="section-heading">
          <div>
            <h2 className="section-title">Assessment History</h2>
            <p className="section-subtitle">View score, submission time, and answer review.</p>
          </div>
        </div>

        {!data?.attempts?.length ? (
          <div className="empty-state mt-2">
            <p>No assessment attempts yet.</p>
          </div>
        ) : (
          <div className="card-grid mt-2">
            {data.attempts.map((attempt) => (
              <article key={attempt.id} className="card">
                <h3>{attempt.assessment?.title || "Assessment"}</h3>
                <p>Score: {attempt.score}</p>
                <p>Time Taken: {attempt.totalQuestions ? `${attempt.correctAnswers}/${attempt.totalQuestions}` : "N/A"}</p>
                <p style={{ color: "var(--text-muted)" }}>
                  {attempt.endTime ? new Date(attempt.endTime).toLocaleString() : "Not submitted"}
                </p>
                <div className="card-actions">
                  <button className="btn btn-secondary btn-sm" onClick={() => openReview(attempt.id)}>
                    Review Answers
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      <section className="surface-panel">
        <div className="section-heading">
          <div>
            <h2 className="section-title">Practice History</h2>
          </div>
        </div>
        {!data?.practiceHistory?.length ? (
          <div className="empty-state mt-2">
            <p>No practice attempts yet.</p>
          </div>
        ) : (
          <div className="card-grid mt-2">
            {data.practiceHistory.slice(0, 10).map((entry, index) => (
              <article key={`${entry.attemptedAt}-${index}`} className="card">
                <h3>{entry.topic}</h3>
                <p>{entry.correct ? "Correct" : "Incorrect"}</p>
                <p style={{ color: "var(--text-muted)" }}>
                  {entry.attemptedAt ? new Date(entry.attemptedAt).toLocaleString() : "N/A"}
                </p>
              </article>
            ))}
          </div>
        )}
      </section>

      {reviewError && <div className="alert alert-error">{reviewError}</div>}
      {reviewData && (
        <section className="surface-panel">
          <div className="section-heading">
            <div>
              <h2 className="section-title">Review Answers</h2>
            </div>
            <button className="btn btn-danger btn-sm" onClick={() => setReviewData(null)}>
              Close
            </button>
          </div>
          <div className="card-grid mt-2">
            {reviewData.answers?.map((answer) => (
              <article key={answer.questionId} className="card">
                <h3>{answer.questionText}</h3>
                <p style={{ color: answer.correct ? "var(--success)" : "var(--danger)" }}>
                  Your answer: {answer.yourAnswer || "Not answered"}
                </p>
                <p>Correct answer: {answer.correctAnswer}</p>
                <p>{answer.explanation || "No explanation available."}</p>
              </article>
            ))}
          </div>
        </section>
      )}
    </Layout>
  );
}
