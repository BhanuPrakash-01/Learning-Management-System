import { useEffect, useRef, useState } from "react";
import Layout from "../../components/Layout";
import {
  bulkUploadQuestions,
  createQuestion,
  deleteQuestion,
  getAdminAssessments,
  getAdminQuestions,
  updateQuestion,
} from "../../services/adminService";

// ✅ Generates and downloads a sample CSV template so admins know the exact
// format without reading documentation. This is the UX pattern used by
// Shopify, Stripe, and most SaaS platforms with bulk-import features.
function downloadCsvTemplate() {
  const header = "questionText,optionA,optionB,optionC,optionD,correctAnswer,assessmentId";
  const example1 = '"What is Java?","A programming language","A type of coffee","An island","A car","A",1';
  const example2 = '"What does JVM stand for?","Java Virtual Machine","Java Version Manager","Java Verified Module","None","A",1';
  const example3 = '"Which keyword creates a class instance?","new","create","make","build","A",1';

  const csvContent = [header, example1, example2, example3].join("\n");
  const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);

  const link = document.createElement("a");
  link.href = url;
  link.download = "questions_template.csv";
  link.click();

  URL.revokeObjectURL(url);
}

export default function Questions() {
  const [questions, setQuestions] = useState([]);
  const [assessments, setAssessments] = useState([]);

  // ── Single-question form ──────────────────────────────────────────────────
  const [form, setForm] = useState({
    questionText: "",
    optionA: "",
    optionB: "",
    optionC: "",
    optionD: "",
    correctAnswer: "",
    assessmentId: "",
  });
  const [editId, setEditId] = useState(null);

  // ── Bulk upload state ─────────────────────────────────────────────────────
  // uploadStatus: 'idle' | 'uploading' | 'success' | 'error'
  const [uploadStatus, setUploadStatus] = useState("idle");
  const [uploadMessage, setUploadMessage] = useState("");
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [questionsRes, assessmentsRes] = await Promise.all([
        getAdminQuestions(),
        getAdminAssessments(),
      ]);
      setQuestions(questionsRes.data);
      setAssessments(assessmentsRes.data);
    } catch (error) {
      console.error("Failed to load data:", error);
    }
  };

  const resetForm = () => {
    setForm({
      questionText: "",
      optionA: "",
      optionB: "",
      optionC: "",
      optionD: "",
      correctAnswer: "",
      assessmentId: "",
    });
    setEditId(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!form.assessmentId) {
      alert("Please select an assessment");
      return;
    }
    if (!form.correctAnswer) {
      alert("Please select the correct answer");
      return;
    }

    try {
      const data = { ...form, assessmentId: parseInt(form.assessmentId, 10) };
      if (editId) {
        await updateQuestion(editId, data);
      } else {
        await createQuestion(data);
      }
      resetForm();
      loadData();
    } catch (error) {
      alert(`Error: ${error.response?.data?.error || error.message}`);
    }
  };

  const handleEdit = (question) => {
    setForm({
      questionText: question.questionText,
      optionA: question.optionA || "",
      optionB: question.optionB || "",
      optionC: question.optionC || "",
      optionD: question.optionD || "",
      correctAnswer: question.correctAnswer || "",
      assessmentId: question.assessment?.id?.toString() || "",
    });
    setEditId(question.id);
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Delete this question?")) return;
    try {
      await deleteQuestion(id);
      loadData();
    } catch (error) {
      alert(`Error deleting: ${error.response?.data || error.message}`);
    }
  };

  // ── Bulk upload handlers ──────────────────────────────────────────────────

  // Processes the selected or dropped file.
  // We validate client-side that it's a .csv before sending to the server —
  // this gives instant feedback without a network round-trip.
  const processFile = async (file) => {
    if (!file) return;

    if (!file.name.endsWith(".csv")) {
      setUploadStatus("error");
      setUploadMessage("Please upload a .csv file. Download the template to see the correct format.");
      return;
    }

    setUploadStatus("uploading");
    setUploadMessage("Uploading and validating your questions…");

    try {
      const res = await bulkUploadQuestions(file);
      setUploadStatus("success");
      setUploadMessage(res.data.message);
      loadData(); // Refresh the question list
    } catch (error) {
      setUploadStatus("error");
      // The server returns detailed row-level error messages —
      // show them so the admin knows exactly what to fix
      const serverMsg = error.response?.data?.error || error.message;
      setUploadMessage(serverMsg);
    }
  };

  const handleFileInput = (e) => {
    processFile(e.target.files[0]);
    // Reset input so the same file can be re-uploaded after fixes
    e.target.value = "";
  };

  // Drag-and-drop handlers
  const handleDragOver = (e) => {
    e.preventDefault();
    setDragOver(true);
  };
  const handleDragLeave = () => setDragOver(false);
  const handleDrop = (e) => {
    e.preventDefault();
    setDragOver(false);
    processFile(e.dataTransfer.files[0]);
  };

  const uploadStatusColor = {
    idle: "var(--text-muted)",
    uploading: "var(--warning)",
    success: "var(--success)",
    error: "var(--danger)",
  };

  return (
    <Layout>
      {/* ── Hero ─────────────────────────────────────────────────────────── */}
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Question bank</div>
          <h1 className="page-title">Build and maintain assessment questions.</h1>
          <p className="page-subtitle">
            Add questions one at a time or upload hundreds at once via CSV — just like Canvas and Moodle.
          </p>
        </div>
        <div className="hero-aside">
          <div className="hero-badge">
            <span className="hero-badge-label">Questions</span>
            <span className="hero-badge-value">{questions.length}</span>
          </div>
        </div>
      </section>

      {/* ── Bulk Upload Panel ─────────────────────────────────────────────── */}
      {/* WHY THIS DESIGN:
            Real-world LMS platforms put bulk import as a prominent feature, not
            hidden in a settings menu. Drag-and-drop + file browser mirrors what
            admins already know from Google Drive and Dropbox.
            The template download ensures zero ambiguity about the format. */}
      <section className="form-section">
        <h3>📥 Bulk import via CSV</h3>
        <p className="form-intro">
          Prepare your questions in Excel or Google Sheets, export as CSV, and upload.
          Download the template below to see the exact column format required.
        </p>

        {/* Template download */}
        <div style={{ marginBottom: "1rem" }}>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={downloadCsvTemplate}
          >
            ⬇ Download CSV Template
          </button>
          <span style={{ marginLeft: "0.75rem", color: "var(--text-muted)", fontSize: "0.88rem" }}>
            Open in Excel or Google Sheets, fill in your questions, save as .csv, then upload.
          </span>
        </div>

        {/* Drag-and-drop upload zone */}
        <div
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
          style={{
            border: `2px dashed ${dragOver ? "var(--primary)" : "var(--border-strong)"}`,
            borderRadius: "var(--radius-md)",
            padding: "2rem",
            textAlign: "center",
            cursor: "pointer",
            background: dragOver ? "var(--primary-soft)" : "var(--surface-muted)",
            transition: "all var(--transition)",
          }}
        >
          <div style={{ fontSize: "2.5rem", marginBottom: "0.5rem" }}>📂</div>
          <p style={{ fontWeight: 700, marginBottom: "0.25rem" }}>
            {dragOver ? "Drop your CSV here" : "Drag & drop your CSV file here"}
          </p>
          <p style={{ color: "var(--text-muted)", fontSize: "0.88rem" }}>
            or click to browse
          </p>
          <input
            ref={fileInputRef}
            type="file"
            accept=".csv"
            onChange={handleFileInput}
            style={{ display: "none" }}
          />
        </div>

        {/* Upload status feedback */}
        {uploadStatus !== "idle" && (
          <div
            style={{
              marginTop: "1rem",
              padding: "0.9rem 1rem",
              borderRadius: "14px",
              border: `1px solid ${uploadStatusColor[uploadStatus]}22`,
              background:
                uploadStatus === "success"
                  ? "var(--success-soft)"
                  : uploadStatus === "error"
                  ? "var(--danger-soft)"
                  : "var(--warning-soft)",
              color: uploadStatusColor[uploadStatus],
              fontSize: "0.9rem",
              whiteSpace: "pre-wrap", // Preserve line breaks in multi-row error messages
            }}
          >
            {uploadStatus === "uploading" && "⏳ "}
            {uploadStatus === "success" && "✅ "}
            {uploadStatus === "error" && "❌ "}
            {uploadMessage}
          </div>
        )}

        {/* CSV Format reminder */}
        <div
          style={{
            marginTop: "1rem",
            padding: "0.85rem 1rem",
            borderRadius: "var(--radius-sm)",
            background: "var(--background-strong)",
            fontSize: "0.83rem",
            color: "var(--text-muted)",
            fontFamily: "monospace",
          }}
        >
          <strong style={{ fontFamily: "Inter, sans-serif", color: "var(--text-soft)" }}>
            CSV column order:
          </strong>
          <br />
          questionText, optionA, optionB, optionC, optionD, correctAnswer (A/B/C/D), assessmentId
        </div>
      </section>

      {/* ── Single Question Form ──────────────────────────────────────────── */}
      <section className="form-section">
        <h3>{editId ? "Edit question" : "➕ Add a single question"}</h3>
        <p className="form-intro">Define the prompt, four options, and the correct answer.</p>

        <form onSubmit={handleSubmit}>
          <div className="form-grid">
            <div className="form-group form-group-full">
              <label htmlFor="question-text">Question text</label>
              <textarea
                id="question-text"
                placeholder="Enter the question"
                value={form.questionText}
                onChange={(e) => setForm({ ...form, questionText: e.target.value })}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="question-assessment">Assessment</label>
              <select
                id="question-assessment"
                value={form.assessmentId}
                onChange={(e) => setForm({ ...form, assessmentId: e.target.value })}
                required
              >
                <option value="">Select assessment</option>
                {assessments.map((assessment) => (
                  <option key={assessment.id} value={assessment.id}>
                    {assessment.title}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="question-correct">Correct answer</label>
              <select
                id="question-correct"
                value={form.correctAnswer}
                onChange={(e) => setForm({ ...form, correctAnswer: e.target.value })}
                required
              >
                <option value="">Select answer</option>
                <option value="A">A</option>
                <option value="B">B</option>
                <option value="C">C</option>
                <option value="D">D</option>
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="option-a">Option A</label>
              <input
                id="option-a"
                placeholder="Option A"
                value={form.optionA}
                onChange={(e) => setForm({ ...form, optionA: e.target.value })}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="option-b">Option B</label>
              <input
                id="option-b"
                placeholder="Option B"
                value={form.optionB}
                onChange={(e) => setForm({ ...form, optionB: e.target.value })}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="option-c">Option C</label>
              <input
                id="option-c"
                placeholder="Option C"
                value={form.optionC}
                onChange={(e) => setForm({ ...form, optionC: e.target.value })}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="option-d">Option D</label>
              <input
                id="option-d"
                placeholder="Option D"
                value={form.optionD}
                onChange={(e) => setForm({ ...form, optionD: e.target.value })}
                required
              />
            </div>
          </div>

          <div className="card-actions">
            <button type="submit" className="btn btn-primary">
              {editId ? "Update Question" : "Add Question"}
            </button>
            {editId && (
              <button type="button" className="btn btn-secondary" onClick={resetForm}>
                Cancel
              </button>
            )}
          </div>
        </form>
      </section>

      {/* ── Question List ─────────────────────────────────────────────────── */}
      {questions.length === 0 ? (
        <div className="empty-state">
          <p>No questions yet. Add one above or bulk-upload a CSV.</p>
        </div>
      ) : (
        <div className="card-grid">
          {questions.map((question) => (
            <article key={question.id} className="card">
              <div className="list-card-top">
                <div>
                  <h3>{question.questionText}</h3>
                  <p>{question.assessment?.title || "No linked assessment"}</p>
                </div>
                <span className="badge badge-success">Correct: {question.correctAnswer}</span>
              </div>

              <div className="card-meta">
                <div className="meta-item">
                  <span>A</span>
                  <strong>{question.optionA}</strong>
                </div>
                <div className="meta-item">
                  <span>B</span>
                  <strong>{question.optionB}</strong>
                </div>
                <div className="meta-item">
                  <span>C</span>
                  <strong>{question.optionC}</strong>
                </div>
                <div className="meta-item">
                  <span>D</span>
                  <strong>{question.optionD}</strong>
                </div>
              </div>

              <div className="card-actions">
                <button className="btn btn-secondary btn-sm" onClick={() => handleEdit(question)}>
                  Edit
                </button>
                <button
                  className="btn btn-danger btn-sm"
                  onClick={() => handleDelete(question.id)}
                >
                  Delete
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </Layout>
  );
}