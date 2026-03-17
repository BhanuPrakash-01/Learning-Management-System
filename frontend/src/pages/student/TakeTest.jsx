import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Layout from "../../components/Layout";
import { saveAnswer, startAttempt, submitAttempt } from "../../services/attemptService";
import { getQuestionsByAssessment } from "../../services/questionService";

export default function TakeTest() {
  const { assessmentId } = useParams();
  const navigate = useNavigate();

  const [attempt, setAttempt] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [answers, setAnswers] = useState({});
  const [timeLeft, setTimeLeft] = useState(null);
  const [submitted, setSubmitted] = useState(false);
  const [score, setScore] = useState(null);
  const [loading, setLoading] = useState(true);
  const timerRef = useRef(null);

  useEffect(() => {
    initTest();

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, []);

  const initTest = async () => {
    try {
      const [attemptRes, questionsRes] = await Promise.all([
        startAttempt(assessmentId),
        getQuestionsByAssessment(assessmentId),
      ]);

      setAttempt(attemptRes.data);
      setQuestions(questionsRes.data);

      if (attemptRes.data.submitted) {
        setSubmitted(true);
        setScore(attemptRes.data.score);
      } else {
        const deadline = new Date(attemptRes.data.deadline);
        const now = new Date();
        const diff = Math.max(0, Math.floor((deadline - now) / 1000));
        setTimeLeft(diff);

        timerRef.current = setInterval(() => {
          setTimeLeft((prev) => {
            if (prev <= 1) {
              clearInterval(timerRef.current);
              handleSubmit();
              return 0;
            }

            return prev - 1;
          });
        }, 1000);
      }
    } catch (error) {
      console.error("Error starting test:", error);
      alert(`Error starting test: ${error.response?.data || error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleAnswer = async (questionId, selectedAnswer) => {
    setAnswers((prev) => ({ ...prev, [questionId]: selectedAnswer }));

    try {
      await saveAnswer(attempt.id, { questionId, selectedAnswer });
    } catch (error) {
      console.error("Error saving answer:", error);
    }
  };

  const handleSubmit = async () => {
    if (submitted || !attempt?.id) return;

    if (timerRef.current) clearInterval(timerRef.current);

    try {
      const res = await submitAttempt(attempt.id);
      setSubmitted(true);
      setScore(res.data.score);
    } catch (error) {
      console.error("Error submitting:", error);
    }
  };

  const formatTime = (seconds) => {
    if (seconds === null) return "--:--";
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
  };

  if (loading) {
    return (
      <Layout>
        <div className="loading">Loading assessment...</div>
      </Layout>
    );
  }

  if (submitted) {
    const total = questions.length || 0;
    let summary = "Keep practicing and review the topics you missed.";

    if (score === total) summary = "Perfect score. Excellent work.";
    else if (score >= total * 0.7) summary = "Strong performance. You handled this assessment well.";
    else if (score >= total * 0.4) summary = "A fair result. Review a few topics and try again.";

    return (
      <Layout>
        <div className="score-card">
          <div className="eyebrow">Assessment submitted</div>
          <h2 className="section-title">Your test is complete.</h2>
          <div className="score-value">
            {score}/{total}
          </div>
          <div className="score-label">{summary}</div>

          <div className="card-actions" style={{ justifyContent: "center" }}>
            <button className="btn btn-primary" onClick={() => navigate("/student/assessments")}>
              Back to Assessments
            </button>
            <button className="btn btn-secondary" onClick={() => navigate("/student/results")}>
              View Results
            </button>
          </div>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Live assessment</div>
          <h1 className="page-title">{attempt?.assessment?.title || "Assessment"}</h1>
          <p className="page-subtitle">{questions.length} questions. Progress is saved as you answer.</p>
        </div>
        <div className="timer">Time Left {formatTime(timeLeft)}</div>
      </section>

      {questions.map((question, index) => (
        <article key={question.id} className="question-card">
          <h4>
            <span className="q-number">Q{index + 1}</span> {question.questionText}
          </h4>

          <div className="options-list">
            {["A", "B", "C", "D"].map((optionKey) => (
              <label
                key={optionKey}
                className={`option-label ${answers[question.id] === optionKey ? "selected" : ""}`}
              >
                <input
                  type="radio"
                  name={`q-${question.id}`}
                  checked={answers[question.id] === optionKey}
                  onChange={() => handleAnswer(question.id, optionKey)}
                />
                <span>
                  <strong>{optionKey}.</strong> {question[`option${optionKey}`]}
                </span>
              </label>
            ))}
          </div>
        </article>
      ))}

      <div className="surface-panel" style={{ textAlign: "center" }}>
        <button
          className="btn btn-success"
          onClick={() => {
            if (window.confirm("Are you sure you want to submit this assessment?")) {
              handleSubmit();
            }
          }}
        >
          Submit Assessment
        </button>
      </div>
    </Layout>
  );
}
