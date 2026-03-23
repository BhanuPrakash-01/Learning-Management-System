import { useEffect, useState } from "react";
import Layout from "../../components/Layout";
import {
  bulkUploadQuestions,
  createAssessment,
  createQuestion,
  deleteAssessment,
  getAdminAssessments,
  getQuestionLibrary,
} from "../../services/adminService";

const assessmentTypes = ["WEEKLY_TEST", "BATCH_ASSESSMENT", "MOCK_TEST", "QAR", "INTERNAL_EXAM"];

export default function ContentHub() {
  const [assessments, setAssessments] = useState([]);
  const [library, setLibrary] = useState([]);
  const [tab, setTab] = useState("assessment");
  const [assessmentForm, setAssessmentForm] = useState({
    title: "",
    description: "",
    duration: 30,
    targetBranches: "",
    targetBatchYears: "",
    targetSections: "",
    assessmentType: "WEEKLY_TEST",
    startTime: "",
    endTime: "",
    allowLateSubmission: false,
    maxAttempts: 1,
    negativeMarking: false,
    penaltyFraction: 0.25,
  });
  const [questionForm, setQuestionForm] = useState({
    questionText: "",
    optionA: "",
    optionB: "",
    optionC: "",
    optionD: "",
    correctAnswer: "A",
    subject: "",
    topic: "",
    difficulty: "Medium",
    assessmentId: "",
  });
  const [bulkFile, setBulkFile] = useState(null);
  const [message, setMessage] = useState("");
  const [uploadError, setUploadError] = useState("");

  const load = async () => {
    const [assessRes, libraryRes] = await Promise.all([
      getAdminAssessments(),
      getQuestionLibrary(),
    ]);
    setAssessments(assessRes.data || []);
    setLibrary(libraryRes.data || []);
  };

  useEffect(() => {
    load().catch((err) => console.error(err));
  }, []);

  const submitAssessment = async (e) => {
    e.preventDefault();
    setMessage("");
    await createAssessment({
      ...assessmentForm,
      duration: Number(assessmentForm.duration),
      targetBranches: assessmentForm.targetBranches
        .split(",")
        .map((item) => item.trim())
        .filter(Boolean),
      targetBatchYears: assessmentForm.targetBatchYears
        .split(",")
        .map((item) => Number(item.trim()))
        .filter(Boolean),
      targetSections: assessmentForm.targetSections
        .split(",")
        .map((item) => item.trim())
        .filter(Boolean),
      maxAttempts: Number(assessmentForm.maxAttempts),
      penaltyFraction: Number(assessmentForm.penaltyFraction),
    });
    setMessage("Assessment saved.");
    load();
  };

  const submitQuestion = async (e) => {
    e.preventDefault();
    setMessage("");
    await createQuestion({
      ...questionForm,
      assessmentId: Number(questionForm.assessmentId),
    });
    setMessage("Question created.");
    setQuestionForm((prev) => ({ ...prev, questionText: "", optionA: "", optionB: "", optionC: "", optionD: "" }));
    load();
  };

  const uploadBulk = async () => {
    if (!bulkFile) return;
    setMessage("");
    setUploadError("");
    try {
      const res = await bulkUploadQuestions(bulkFile);
      setMessage(res.data.message || "Bulk upload completed.");
      setBulkFile(null);
      load();
    } catch (error) {
      const backendError = error.response?.data?.error;
      setUploadError(backendError || "Bulk upload failed. Please verify CSV format and assessment IDs.");
    }
  };

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Content Hub</div>
          <h1 className="page-title">Targeted assessments and question library</h1>
          <p className="page-subtitle">
            Replace legacy course-assessment CRUD with branch/batch-aware assessment targeting.
          </p>
        </div>
      </section>

      <section className="surface-panel">
        <div className="card-actions">
          <button className={`btn ${tab === "assessment" ? "btn-primary" : "btn-secondary"} btn-sm`} onClick={() => setTab("assessment")}>
            Assessment Builder
          </button>
          <button className={`btn ${tab === "single" ? "btn-primary" : "btn-secondary"} btn-sm`} onClick={() => setTab("single")}>
            Add Single
          </button>
          <button className={`btn ${tab === "bulk" ? "btn-primary" : "btn-secondary"} btn-sm`} onClick={() => setTab("bulk")}>
            Bulk CSV Upload
          </button>
          <button className={`btn ${tab === "library" ? "btn-primary" : "btn-secondary"} btn-sm`} onClick={() => setTab("library")}>
            Question Library
          </button>
        </div>
      </section>

      {tab === "assessment" && (
        <section className="surface-panel">
          <form onSubmit={submitAssessment}>
            <div className="form-grid">
              <div className="form-group">
                <label htmlFor="title">Title</label>
                <input id="title" value={assessmentForm.title} onChange={(e) => setAssessmentForm((prev) => ({ ...prev, title: e.target.value }))} required />
              </div>
              <div className="form-group">
                <label htmlFor="type">Type</label>
                <select id="type" value={assessmentForm.assessmentType} onChange={(e) => setAssessmentForm((prev) => ({ ...prev, assessmentType: e.target.value }))}>
                  {assessmentTypes.map((type) => (
                    <option key={type} value={type}>
                      {type}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label htmlFor="duration">Duration (minutes)</label>
                <input id="duration" type="number" value={assessmentForm.duration} onChange={(e) => setAssessmentForm((prev) => ({ ...prev, duration: e.target.value }))} />
              </div>
              <div className="form-group form-group-full">
                <label htmlFor="desc">Description</label>
                <textarea id="desc" value={assessmentForm.description} onChange={(e) => setAssessmentForm((prev) => ({ ...prev, description: e.target.value }))} />
              </div>
              <div className="form-group">
                <label htmlFor="branches">Target Branches (comma-separated)</label>
                <input id="branches" value={assessmentForm.targetBranches} onChange={(e) => setAssessmentForm((prev) => ({ ...prev, targetBranches: e.target.value }))} placeholder="CSE,IT,DS" />
              </div>
              <div className="form-group">
                <label htmlFor="batches">Target Batch Years (comma-separated)</label>
                <input id="batches" value={assessmentForm.targetBatchYears} onChange={(e) => setAssessmentForm((prev) => ({ ...prev, targetBatchYears: e.target.value }))} placeholder="2024,2025" />
              </div>
              <div className="form-group">
                <label htmlFor="sections">Target Sections (optional)</label>
                <input id="sections" value={assessmentForm.targetSections} onChange={(e) => setAssessmentForm((prev) => ({ ...prev, targetSections: e.target.value }))} placeholder="A,B" />
              </div>
              <div className="form-group">
                <label htmlFor="start">Start Time</label>
                <input id="start" type="datetime-local" value={assessmentForm.startTime} onChange={(e) => setAssessmentForm((prev) => ({ ...prev, startTime: e.target.value }))} />
              </div>
              <div className="form-group">
                <label htmlFor="end">End Time</label>
                <input id="end" type="datetime-local" value={assessmentForm.endTime} onChange={(e) => setAssessmentForm((prev) => ({ ...prev, endTime: e.target.value }))} />
              </div>
              <div className="form-group">
                <label htmlFor="maxAttempts">Max Attempts</label>
                <input id="maxAttempts" type="number" value={assessmentForm.maxAttempts} onChange={(e) => setAssessmentForm((prev) => ({ ...prev, maxAttempts: e.target.value }))} />
              </div>
              <div className="form-group">
                <label htmlFor="penaltyFraction">Penalty Fraction</label>
                <input id="penaltyFraction" type="number" step="0.01" value={assessmentForm.penaltyFraction} onChange={(e) => setAssessmentForm((prev) => ({ ...prev, penaltyFraction: e.target.value }))} />
              </div>
            </div>

            <div className="card-actions mt-2">
              <label className="btn btn-secondary btn-sm" style={{ cursor: "pointer" }}>
                <input
                  type="checkbox"
                  checked={assessmentForm.allowLateSubmission}
                  onChange={(e) => setAssessmentForm((prev) => ({ ...prev, allowLateSubmission: e.target.checked }))}
                  style={{ marginRight: "0.5rem" }}
                />
                Allow Late Submission
              </label>
              <label className="btn btn-secondary btn-sm" style={{ cursor: "pointer" }}>
                <input
                  type="checkbox"
                  checked={assessmentForm.negativeMarking}
                  onChange={(e) => setAssessmentForm((prev) => ({ ...prev, negativeMarking: e.target.checked }))}
                  style={{ marginRight: "0.5rem" }}
                />
                Negative Marking
              </label>
              <button className="btn btn-primary btn-sm" type="submit">
                Save Assessment
              </button>
            </div>
          </form>

          <div className="card-grid mt-2">
            {assessments.map((assessment) => (
              <article key={assessment.id} className="card">
                <h3>{assessment.title}</h3>
                <p>{assessment.assessmentType}</p>
                <p>{assessment.startTime ? new Date(assessment.startTime).toLocaleString() : "N/A"}</p>
                <div className="card-actions">
                  <button className="btn btn-danger btn-sm" onClick={() => deleteAssessment(assessment.id).then(load)}>
                    Delete
                  </button>
                </div>
              </article>
            ))}
          </div>
        </section>
      )}

      {tab === "single" && (
        <section className="surface-panel">
          <form onSubmit={submitQuestion}>
            <div className="form-grid">
              <div className="form-group form-group-full">
                <label htmlFor="questionText">Question Text</label>
                <textarea id="questionText" value={questionForm.questionText} onChange={(e) => setQuestionForm((prev) => ({ ...prev, questionText: e.target.value }))} required />
              </div>
              <div className="form-group">
                <label htmlFor="assessment">Assessment</label>
                <select id="assessment" value={questionForm.assessmentId} onChange={(e) => setQuestionForm((prev) => ({ ...prev, assessmentId: e.target.value }))} required>
                  <option value="">Select assessment</option>
                  {assessments.map((assessment) => (
                    <option key={assessment.id} value={assessment.id}>
                      {assessment.title}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label htmlFor="correct">Correct Answer</label>
                <select id="correct" value={questionForm.correctAnswer} onChange={(e) => setQuestionForm((prev) => ({ ...prev, correctAnswer: e.target.value }))}>
                  {["A", "B", "C", "D"].map((value) => (
                    <option key={value} value={value}>
                      {value}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label htmlFor="subject">Subject</label>
                <input id="subject" value={questionForm.subject} onChange={(e) => setQuestionForm((prev) => ({ ...prev, subject: e.target.value }))} />
              </div>
              <div className="form-group">
                <label htmlFor="topic">Topic</label>
                <input id="topic" value={questionForm.topic} onChange={(e) => setQuestionForm((prev) => ({ ...prev, topic: e.target.value }))} />
              </div>
              <div className="form-group">
                <label htmlFor="difficulty">Difficulty</label>
                <select id="difficulty" value={questionForm.difficulty} onChange={(e) => setQuestionForm((prev) => ({ ...prev, difficulty: e.target.value }))}>
                  {["Easy", "Medium", "Hard"].map((value) => (
                    <option key={value} value={value}>
                      {value}
                    </option>
                  ))}
                </select>
              </div>
              {["A", "B", "C", "D"].map((option) => (
                <div key={option} className="form-group">
                  <label htmlFor={`option${option}`}>Option {option}</label>
                  <input
                    id={`option${option}`}
                    value={questionForm[`option${option}`]}
                    onChange={(e) => setQuestionForm((prev) => ({ ...prev, [`option${option}`]: e.target.value }))}
                    required
                  />
                </div>
              ))}
            </div>
            <div className="card-actions mt-2">
              <button className="btn btn-primary btn-sm" type="submit">
                Add Question
              </button>
            </div>
          </form>
        </section>
      )}

      {tab === "bulk" && (
        <section className="surface-panel">
          <p className="section-subtitle">
            CSV columns: <strong>questionText, optionA, optionB, optionC, optionD, correctAnswer, assessmentId</strong>
            <br />
            Optional columns: <strong>subject, topic, difficulty</strong>
          </p>

          <div className="form-group">
            <label htmlFor="bulk-file">Upload CSV</label>
            <input id="bulk-file" type="file" accept=".csv" onChange={(e) => setBulkFile(e.target.files?.[0] || null)} />
          </div>
          {bulkFile && (
            <div className="card-actions">
              <button className="btn btn-primary btn-sm" onClick={uploadBulk}>
                Upload {bulkFile.name}
              </button>
            </div>
          )}

          {!!assessments.length && (
            <div className="mt-2" style={{ overflowX: "auto" }}>
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Assessment ID</th>
                    <th>Assessment Title</th>
                  </tr>
                </thead>
                <tbody>
                  {assessments.map((assessment) => (
                    <tr key={assessment.id}>
                      <td>{assessment.id}</td>
                      <td>{assessment.title}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {uploadError && (
            <div className="alert alert-error mt-2" style={{ whiteSpace: "pre-wrap" }}>
              {uploadError}
            </div>
          )}
        </section>
      )}

      {tab === "library" && (
        <section className="surface-panel">
          {!library.length ? (
            <div className="empty-state">
              <p>No questions in library yet.</p>
            </div>
          ) : (
            <div className="card-grid">
              {library.map((question) => (
                <article key={question.id} className="card">
                  <h3>{question.questionText}</h3>
                  <p>{question.subject || "General"} • {question.topic || "General"} • {question.difficulty || "Medium"}</p>
                  <span className="badge badge-success">Correct: {question.correctAnswer}</span>
                </article>
              ))}
            </div>
          )}
        </section>
      )}

      {message && <div className="alert alert-success">{message}</div>}
    </Layout>
  );
}
