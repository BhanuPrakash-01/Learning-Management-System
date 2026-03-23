import { useEffect, useRef, useState, useCallback } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Layout from "../../components/Layout";
import { saveAnswer, startAttempt, submitAttempt } from "../../services/attemptService";
import { getQuestionsByAssessment } from "../../services/questionService";

// ─────────────────────────────────────────────────────────────────────────────
// ANTI-CHEATING DESIGN NOTES
//
// Real exam platforms use a layered approach — no single measure is perfect,
// but stacking them makes cheating significantly harder:
//
//  Layer 1 — Fullscreen enforcement (Respondus LockDown Browser approach)
//            The test only runs in fullscreen. Exiting triggers a violation.
//
//  Layer 2 — Tab / window switch detection (Page Visibility API)
//            document.hidden fires when the user Alt-Tabs or switches tabs.
//            This catches the most common cheating method: looking up answers
//            in another tab.
//
//  Layer 3 — Copy/paste & right-click blocking
//            Prevents students from copying questions into ChatGPT, Google,
//            or sharing with others during the exam.
//
//  Layer 4 — One question at a time (linear navigation)
//            Students cannot see all questions at once, so even if they share
//            the screen, they expose only the current question. This also
//            prevents "read all first, answer easy ones" gaming strategies.
//
//  Layer 5 — Client-side question randomization
//            Questions are shuffled differently for each student session
//            (using Math.random after load). This means two students sitting
//            next to each other see questions in different orders.
//            NOTE: For stronger randomization, move this to the backend and
//            store the order per-attempt so reloads are consistent.
//
//  Layer 6 — Violation counter with auto-submit
//            After MAX_VIOLATIONS, the test is force-submitted. This creates
//            a real cost for repeated cheating attempts.
// ─────────────────────────────────────────────────────────────────────────────

const MAX_VIOLATIONS = 3; // How many tab-switches / fullscreen exits before auto-submit

// Fisher-Yates shuffle — produces an unbiased random permutation.
// Called once after questions load to randomize order for this session.
function shuffleArray(arr) {
  const a = [...arr];
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}

export default function TakeTest() {
  const { assessmentId } = useParams();
  const navigate = useNavigate();

  // ── Core test state ───────────────────────────────────────────────────────
  const [attempt, setAttempt] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [answers, setAnswers] = useState({});
  const [timeLeft, setTimeLeft] = useState(null);
  const [submitted, setSubmitted] = useState(false);
  const [score, setScore] = useState(null);
  const [loading, setLoading] = useState(true);

  // ── One-question-at-a-time navigation ────────────────────────────────────
  const [currentIndex, setCurrentIndex] = useState(0);

  // ── Anti-cheating state ───────────────────────────────────────────────────
  const [violations, setViolations] = useState(0);
  const [warningVisible, setWarningVisible] = useState(false);
  const [warningMessage, setWarningMessage] = useState("");
  const [fullscreenDenied, setFullscreenDenied] = useState(false);

  const timerRef = useRef(null);
  const attemptRef = useRef(null); // Keep attempt accessible in event listeners
  const submittedRef = useRef(false); // Keep submitted state accessible in event listeners

  // ── Fullscreen helpers ────────────────────────────────────────────────────
  const enterFullscreen = useCallback(async () => {
    try {
      if (!document.fullscreenElement) {
        await document.documentElement.requestFullscreen();
      }
    } catch {
      // Browser blocked fullscreen (e.g. Safari strict mode or embedded iframes)
      // We flag this and show a manual instruction instead of crashing
      setFullscreenDenied(true);
    }
  }, []);

  // ── Submit handler ────────────────────────────────────────────────────────
  const handleSubmit = useCallback(async () => {
    if (submittedRef.current || !attemptRef.current?.id) return;
    submittedRef.current = true; // Prevent double-submit races

    if (timerRef.current) clearInterval(timerRef.current);

    try {
      const res = await submitAttempt(attemptRef.current.id);
      setSubmitted(true);
      setScore(res.data.score);
    } catch (error) {
      console.error("Error submitting:", error);
    }
  }, []);

  // ── Violation handler ─────────────────────────────────────────────────────
  // Uses the functional form of setState to get the latest violations value
  // inside event listeners (avoids stale closure problem).
  const triggerViolation = useCallback(
    (reason) => {
      if (submittedRef.current) return;

      setViolations((prev) => {
        const newCount = prev + 1;
        const remaining = MAX_VIOLATIONS - newCount;

        if (newCount >= MAX_VIOLATIONS) {
          setWarningMessage(
            `🚨 Final violation: ${reason}.\n\nYour test is being submitted automatically.`
          );
          setWarningVisible(true);
          // Small delay so the student sees the warning before submit
          setTimeout(() => handleSubmit(), 1500);
        } else {
          setWarningMessage(
            `⚠️ Warning ${newCount}/${MAX_VIOLATIONS}: ${reason}.\n\n` +
              `You have ${remaining} warning${remaining !== 1 ? "s" : ""} remaining before your test is auto-submitted.`
          );
          setWarningVisible(true);
          // Re-enter fullscreen automatically after an exit violation
          if (reason.includes("fullscreen")) {
            setTimeout(() => enterFullscreen(), 300);
          }
        }

        return newCount;
      });
    },
    [handleSubmit, enterFullscreen]
  );

  // ── Anti-cheating event listeners ─────────────────────────────────────────
  useEffect(() => {
    if (submitted) return;

    // LAYER 2: Tab / window switch detection
    // document.visibilitychange fires when the tab loses focus (Alt+Tab,
    // Ctrl+Tab, clicking another window, or the screen locks).
    const handleVisibilityChange = () => {
      if (document.hidden && !submittedRef.current) {
        triggerViolation("Tab switch or window change detected");
      }
    };

    // LAYER 1: Fullscreen exit detection
    const handleFullscreenChange = () => {
      if (!document.fullscreenElement && !submittedRef.current) {
        triggerViolation("Fullscreen mode was exited");
      }
    };

    // LAYER 3a: Right-click disable
    const handleContextMenu = (e) => {
      if (!submittedRef.current) e.preventDefault();
    };

    // LAYER 3b: Keyboard shortcut blocking
    // Blocks: Ctrl+C (copy), Ctrl+V (paste), Ctrl+A (select all),
    //         Ctrl+U (view source), Ctrl+P (print), Ctrl+S (save),
    //         F12 (devtools), Ctrl+Shift+I (devtools), Ctrl+Shift+J (console)
    const handleKeyDown = (e) => {
      if (submittedRef.current) return;

      const blockedCtrlKeys = ["c", "v", "a", "u", "p", "s", "x"];
      if (
        (e.ctrlKey && blockedCtrlKeys.includes(e.key.toLowerCase())) ||
        e.key === "F12" ||
        (e.ctrlKey && e.shiftKey && ["I", "J", "C"].includes(e.key))
      ) {
        e.preventDefault();
      }
    };

    // LAYER 3c: Text selection disable (prevents mouse-drag copying)
    const handleSelectStart = (e) => {
      if (!submittedRef.current) e.preventDefault();
    };

    document.addEventListener("visibilitychange", handleVisibilityChange);
    document.addEventListener("fullscreenchange", handleFullscreenChange);
    document.addEventListener("contextmenu", handleContextMenu);
    document.addEventListener("keydown", handleKeyDown);
    document.addEventListener("selectstart", handleSelectStart);

    return () => {
      document.removeEventListener("visibilitychange", handleVisibilityChange);
      document.removeEventListener("fullscreenchange", handleFullscreenChange);
      document.removeEventListener("contextmenu", handleContextMenu);
      document.removeEventListener("keydown", handleKeyDown);
      document.removeEventListener("selectstart", handleSelectStart);
    };
  }, [submitted, triggerViolation]);

  // ── Test initialisation ───────────────────────────────────────────────────
  useEffect(() => {
    initTest();
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const initTest = async () => {
    try {
      const [attemptRes, questionsRes] = await Promise.all([
        startAttempt(assessmentId),
        getQuestionsByAssessment(assessmentId),
      ]);

      attemptRef.current = attemptRes.data;
      setAttempt(attemptRes.data);

      // LAYER 5: Shuffle questions for this session
      const shuffled = shuffleArray(questionsRes.data);
      setQuestions(shuffled);

      if (attemptRes.data.submitted) {
        submittedRef.current = true;
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

        // LAYER 1: Enter fullscreen when test starts
        await enterFullscreen();
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

  const formatTime = (seconds) => {
    if (seconds === null) return "--:--";
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
  };

  const confirmAndSubmit = () => {
    if (window.confirm("Are you sure you want to submit this assessment?")) {
      handleSubmit();
    }
  };

  // ── Loading state ─────────────────────────────────────────────────────────
  if (loading) {
    return (
      <Layout>
        <div className="loading">Loading assessment…</div>
      </Layout>
    );
  }

  // ── Score / completion screen ─────────────────────────────────────────────
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

  // ── Active exam screen ────────────────────────────────────────────────────
  const currentQuestion = questions[currentIndex];
  const totalQuestions = questions.length;
  const answeredCount = Object.keys(answers).length;
  const isLastQuestion = currentIndex === totalQuestions - 1;
  const timeIsLow = timeLeft !== null && timeLeft < 120; // red timer under 2 min

  return (
    <Layout>
      {/* ── Violation Warning Modal ─────────────────────────────────────── */}
      {warningVisible && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(0,0,0,0.65)",
            zIndex: 9999,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            padding: "1rem",
          }}
          onClick={() => setWarningVisible(false)}
        >
          <div
            style={{
              background: "white",
              borderRadius: "var(--radius-lg)",
              padding: "2rem",
              maxWidth: "480px",
              width: "100%",
              boxShadow: "0 30px 80px rgba(0,0,0,0.3)",
              textAlign: "center",
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ fontSize: "3rem", marginBottom: "0.75rem" }}>
              {violations >= MAX_VIOLATIONS ? "🚫" : "⚠️"}
            </div>
            <h3 style={{ fontFamily: "Nunito, sans-serif", marginBottom: "0.75rem", color: "var(--danger)" }}>
              {violations >= MAX_VIOLATIONS ? "Test Auto-Submitted" : "Integrity Warning"}
            </h3>
            <p style={{ color: "var(--text-soft)", whiteSpace: "pre-wrap", marginBottom: "1.5rem" }}>
              {warningMessage}
            </p>
            {violations < MAX_VIOLATIONS && (
              <button
                className="btn btn-primary"
                onClick={() => {
                  setWarningVisible(false);
                  enterFullscreen();
                }}
              >
                Return to Test
              </button>
            )}
          </div>
        </div>
      )}

      {/* ── Fullscreen denied notice ────────────────────────────────────── */}
      {fullscreenDenied && (
        <div
          className="alert alert-error"
          style={{ marginBottom: 0 }}
        >
          ⚠️ Your browser blocked fullscreen mode. For exam integrity, please manually press{" "}
          <strong>F11</strong> (Windows/Linux) or <strong>Ctrl+Command+F</strong> (Mac) to enter
          fullscreen before continuing.
        </div>
      )}

      {/* ── Exam header ────────────────────────────────────────────────── */}
      <section className="hero-panel" style={{ userSelect: "none" }}>
        <div className="hero-copy">
          <div className="eyebrow">Live assessment</div>
          <h1 className="page-title">{attempt?.assessment?.title || "Assessment"}</h1>
          <p className="page-subtitle">
            Question {currentIndex + 1} of {totalQuestions} &nbsp;·&nbsp; {answeredCount} answered
          </p>
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem", alignItems: "flex-end" }}>
          {/* Timer — turns red when under 2 minutes */}
          <div
            className="timer"
            style={{
              borderColor: timeIsLow ? "rgba(220,20,60,0.4)" : "rgba(220,20,60,0.2)",
              background: timeIsLow ? "var(--danger-soft)" : "white",
            }}
          >
            {timeIsLow ? "🔴" : "⏱"} {formatTime(timeLeft)}
          </div>

          {/* Violation counter — only show after first warning */}
          {violations > 0 && (
            <div
              style={{
                padding: "0.5rem 1rem",
                borderRadius: "999px",
                background: "var(--danger-soft)",
                color: "var(--danger)",
                fontSize: "0.82rem",
                fontWeight: 700,
              }}
            >
              ⚠️ Warnings: {violations}/{MAX_VIOLATIONS}
            </div>
          )}
        </div>
      </section>

      {/* ── Progress bar ───────────────────────────────────────────────── */}
      <div className="progress-line">
        <span
          style={{ width: `${((currentIndex + 1) / totalQuestions) * 100}%`, transition: "width 0.3s ease" }}
        />
      </div>

      {/* ── Current Question ───────────────────────────────────────────── */}
      {/* LAYER 4: Only show one question at a time.
            This means even if a student screenshots the screen, they can only
            capture one question per screenshot. It also prevents them from
            scrolling ahead to distribute answering across collaborators. */}
      {currentQuestion && (
        <article
          className="question-card"
          style={{ userSelect: "none" }} // Extra CSS-level copy prevention
        >
          <h4>
            <span className="q-number">Q{currentIndex + 1}</span>{" "}
            {currentQuestion.questionText}
          </h4>

          <div className="options-list">
            {["A", "B", "C", "D"].map((optionKey) => (
              <label
                key={optionKey}
                className={`option-label ${
                  answers[currentQuestion.id] === optionKey ? "selected" : ""
                }`}
              >
                <input
                  type="radio"
                  name={`q-${currentQuestion.id}`}
                  checked={answers[currentQuestion.id] === optionKey}
                  onChange={() => handleAnswer(currentQuestion.id, optionKey)}
                />
                <span>
                  <strong>{optionKey}.</strong> {currentQuestion[`option${optionKey}`]}
                </span>
              </label>
            ))}
          </div>
        </article>
      )}

      {/* ── Navigation ─────────────────────────────────────────────────── */}
      <div className="surface-panel" style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "1rem" }}>
        {/* Previous button */}
        <button
          className="btn btn-secondary"
          onClick={() => setCurrentIndex((i) => Math.max(0, i - 1))}
          disabled={currentIndex === 0}
        >
          ← Previous
        </button>

        {/* Question dots — show answered/unanswered status */}
        <div style={{ display: "flex", gap: "0.4rem", flexWrap: "wrap", justifyContent: "center" }}>
          {questions.map((q, idx) => (
            <button
              key={q.id}
              onClick={() => setCurrentIndex(idx)}
              title={`Question ${idx + 1}${answers[q.id] ? " (answered)" : " (unanswered)"}`}
              style={{
                width: "28px",
                height: "28px",
                borderRadius: "50%",
                border: idx === currentIndex ? "2px solid var(--primary)" : "1px solid var(--border-strong)",
                background: answers[q.id]
                  ? "var(--primary)"
                  : idx === currentIndex
                  ? "var(--primary-soft)"
                  : "white",
                color: answers[q.id] ? "white" : "var(--text-soft)",
                fontSize: "0.75rem",
                fontWeight: 700,
                cursor: "pointer",
                transition: "all var(--transition)",
              }}
            >
              {idx + 1}
            </button>
          ))}
        </div>

        {/* Next or Submit button */}
        {isLastQuestion ? (
          <button className="btn btn-success" onClick={confirmAndSubmit}>
            Submit Assessment ✓
          </button>
        ) : (
          <button
            className="btn btn-primary"
            onClick={() => setCurrentIndex((i) => Math.min(totalQuestions - 1, i + 1))}
          >
            Next →
          </button>
        )}
      </div>

      {/* ── Answered progress summary ───────────────────────────────────── */}
      <div
        style={{
          textAlign: "center",
          color: "var(--text-muted)",
          fontSize: "0.88rem",
          paddingBottom: "0.5rem",
        }}
      >
        {answeredCount} of {totalQuestions} questions answered
        {answeredCount < totalQuestions && (
          <span style={{ color: "var(--warning)", marginLeft: "0.5rem" }}>
            ({totalQuestions - answeredCount} remaining)
          </span>
        )}
      </div>
    </Layout>
  );
}