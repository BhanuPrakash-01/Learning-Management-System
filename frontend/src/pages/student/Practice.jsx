import { useEffect, useMemo, useState } from "react";
import Layout from "../../components/Layout";
import {
  getPracticeCategories,
  getPracticeProgress,
  getPracticeQuestions,
  submitPracticeAttempt,
} from "../../services/practiceService";

export default function Practice() {
  const [categories, setCategories] = useState([]);
  const [progress, setProgress] = useState([]);
  const [selectedTopic, setSelectedTopic] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [feedback, setFeedback] = useState(null);
  const [mode, setMode] = useState("practice");

  useEffect(() => {
    Promise.all([getPracticeCategories(), getPracticeProgress()])
      .then(([categoriesRes, progressRes]) => {
        setCategories(categoriesRes.data || []);
        setProgress(progressRes.data || []);
      })
      .catch((err) => console.error(err));
  }, []);

  const currentQuestion = questions[currentIndex];

  const topicProgress = useMemo(() => {
    const map = {};
    progress.forEach((item) => {
      map[item.topicId] = item;
    });
    return map;
  }, [progress]);

  const selectTopic = async (topicId, selectedMode = mode) => {
    setSelectedTopic(topicId);
    setMode(selectedMode);
    setFeedback(null);
    setCurrentIndex(0);
    const res = await getPracticeQuestions(topicId, selectedMode);
    setQuestions(res.data || []);
  };

  const handleAnswer = async (answer) => {
    if (!currentQuestion) return;
    const res = await submitPracticeAttempt({
      questionId: currentQuestion.id,
      selectedAnswer: answer,
    });
    setFeedback(res.data);
  };

  const nextQuestion = () => {
    setFeedback(null);
    setCurrentIndex((prev) => Math.min(prev + 1, questions.length - 1));
  };

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Practice</div>
          <h1 className="page-title">Aptitude and technical practice by topic</h1>
          <p className="page-subtitle">
            Train one question at a time in practice mode or switch to 10-question timed topic tests.
          </p>
        </div>
      </section>

      {!selectedTopic ? (
        <section className="surface-panel">
          <div className="section-heading">
            <div>
              <h2 className="section-title">Categories & Topics</h2>
              <p className="section-subtitle">Choose a subtopic to start practicing.</p>
            </div>
          </div>

          <div className="card-grid mt-2">
            {categories.map((category) => (
              <article key={category.id} className="card">
                <h3>{category.name}</h3>
                <div className="card-meta">
                  <div className="meta-item">
                    <span>Topics</span>
                    <strong>{category.topics?.length || 0}</strong>
                  </div>
                </div>
                <div className="card-actions" style={{ flexDirection: "column", alignItems: "stretch" }}>
                  {(category.topics || []).map((topic) => {
                    const itemProgress = topicProgress[topic.id];
                    return (
                      <button
                        key={topic.id}
                        className="btn btn-secondary btn-sm"
                        onClick={() => selectTopic(topic.id, "practice")}
                      >
                        {topic.name}
                        {itemProgress ? ` • ${itemProgress.accuracy}%` : ""}
                        {itemProgress?.completed ? " • ✔" : ""}
                      </button>
                    );
                  })}
                </div>
              </article>
            ))}
          </div>
        </section>
      ) : (
        <section className="surface-panel">
          <div className="section-heading">
            <div>
              <h2 className="section-title">
                Topic Session ({mode === "test" ? "Topic Test" : "Practice Mode"})
              </h2>
              <p className="section-subtitle">
                Question {currentIndex + 1} of {questions.length}
              </p>
            </div>
            <div className="card-actions">
              <button className="btn btn-secondary btn-sm" onClick={() => selectTopic(selectedTopic, "practice")}>
                Practice Mode
              </button>
              <button className="btn btn-secondary btn-sm" onClick={() => selectTopic(selectedTopic, "test")}>
                Topic Test (10Q)
              </button>
              <button
                className="btn btn-danger btn-sm"
                onClick={() => {
                  setSelectedTopic(null);
                  setQuestions([]);
                  setFeedback(null);
                }}
              >
                Exit Topic
              </button>
            </div>
          </div>

          {!currentQuestion ? (
            <div className="empty-state mt-2">
              <p>No questions available for this topic yet.</p>
            </div>
          ) : (
            <article className="card mt-2">
              <h3>{currentQuestion.questionText}</h3>
              <p style={{ color: "var(--text-muted)" }}>Difficulty: {currentQuestion.difficulty || "Medium"}</p>

              <div className="card-actions" style={{ flexDirection: "column", alignItems: "stretch" }}>
                {(JSON.parse(currentQuestion.options || "[]") || []).map((option) => (
                  <button key={option} className="btn btn-secondary" onClick={() => handleAnswer(option)}>
                    {option}
                  </button>
                ))}
              </div>

              {feedback && (
                <div className={`alert ${feedback.correct ? "alert-success" : "alert-error"}`} style={{ marginTop: "1rem" }}>
                  <p>{feedback.correct ? "Correct answer." : "Incorrect answer."}</p>
                  <p>{feedback.explanation || "No explanation available."}</p>
                  <p>
                    Streak: {feedback.currentStreak} (Best: {feedback.bestStreak})
                  </p>
                </div>
              )}

              <div className="card-actions" style={{ marginTop: "1rem" }}>
                <button className="btn btn-primary btn-sm" onClick={nextQuestion} disabled={currentIndex >= questions.length - 1}>
                  Next Question
                </button>
              </div>
            </article>
          )}
        </section>
      )}
    </Layout>
  );
}
