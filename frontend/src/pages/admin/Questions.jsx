import { useEffect, useState } from "react";
import Layout from "../../components/Layout";
import {
  createQuestion,
  deleteQuestion,
  getAdminAssessments,
  getAdminQuestions,
  updateQuestion,
} from "../../services/adminService";

export default function Questions() {
  const [questions, setQuestions] = useState([]);
  const [assessments, setAssessments] = useState([]);
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

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [questionsRes, assessmentsRes] = await Promise.all([getAdminQuestions(), getAdminAssessments()]);
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
      const data = {
        ...form,
        assessmentId: parseInt(form.assessmentId, 10),
      };

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

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Question bank</div>
          <h1 className="page-title">Build and maintain assessment questions.</h1>
          <p className="page-subtitle">
            Add structured multiple-choice questions and connect them to the right assessment.
          </p>
        </div>
        <div className="hero-aside">
          <div className="hero-badge">
            <span className="hero-badge-label">Questions</span>
            <span className="hero-badge-value">{questions.length}</span>
          </div>
        </div>
      </section>

      <section className="form-section">
        <h3>{editId ? "Edit question" : "Create a question"}</h3>
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

      {questions.length === 0 ? (
        <div className="empty-state">
          <p>No questions yet. Add one above.</p>
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
                <span className="badge badge-success">Correct {question.correctAnswer}</span>
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
                <button className="btn btn-danger btn-sm" onClick={() => handleDelete(question.id)}>
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
