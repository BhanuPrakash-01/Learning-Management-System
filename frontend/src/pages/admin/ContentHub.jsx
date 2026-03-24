import { useEffect, useMemo, useState } from "react";
import Layout from "../../components/Layout";
import {
  bulkUploadQuestions,
  createAssessmentWithQuestions,
  createQuestion,
  deleteAssessment,
  deleteQuestion,
  deleteQuestionsBulk,
  getAdminAssessments,
  getAdminCourses,
  getQuestionLibrary,
  updateQuestion,
} from "../../services/adminService";

const assessmentTypes = ["WEEKLY_TEST", "BATCH_ASSESSMENT", "MOCK_TEST", "QAR", "INTERNAL_EXAM"];
const defaultLibrary = { content: [], totalElements: 0, totalPages: 0, page: 0, size: 25, assessmentCounts: {} };

const initialAssessmentForm = {
  title: "",
  description: "",
  duration: 30,
  courseId: "",
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
  reviewAfterClose: false,
};

function csvEscape(value) {
  const raw = String(value ?? "");
  return `"${raw.replace(/"/g, '""')}"`;
}

function buildWizardCsv(rows) {
  const header = "questionText,optionA,optionB,optionC,optionD,correctAnswer,subject,topic,difficulty";
  const lines = rows.map((row) =>
    [
      row.questionText,
      row.optionA,
      row.optionB,
      row.optionC,
      row.optionD,
      row.correctAnswer,
      row.subject,
      row.topic,
      row.difficulty,
    ]
      .map(csvEscape)
      .join(",")
  );
  return [header, ...lines].join("\n");
}

export default function ContentHub() {
  const [assessments, setAssessments] = useState([]);
  const [courses, setCourses] = useState([]);
  const [library, setLibrary] = useState(defaultLibrary);
  const [tab, setTab] = useState("assessment");

  const [filters, setFilters] = useState({
    search: "",
    topic: "",
    difficulty: "",
    assessmentId: "",
    page: 0,
    size: 25,
  });

  const [selectedIds, setSelectedIds] = useState([]);
  const [editingQuestionId, setEditingQuestionId] = useState(null);
  const [editForm, setEditForm] = useState({ questionText: "", topic: "", difficulty: "", correctAnswer: "" });

  const [assessmentForm, setAssessmentForm] = useState(initialAssessmentForm);
  const [wizardStep, setWizardStep] = useState(1);
  const [wizardInputTab, setWizardInputTab] = useState("single");
  const [wizardCsvFile, setWizardCsvFile] = useState(null);
  const [draftQuestions, setDraftQuestions] = useState([]);
  const [draftQuestionForm, setDraftQuestionForm] = useState({
    questionText: "",
    optionA: "",
    optionB: "",
    optionC: "",
    optionD: "",
    correctAnswer: "A",
    subject: "",
    topic: "",
    difficulty: "Medium",
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
  const [error, setError] = useState("");

  const assessmentCounts = library.assessmentCounts || {};

  const topicOptions = useMemo(() => {
    const source = library.content || [];
    return [...new Set(source.map((item) => item.topic).filter(Boolean))];
  }, [library.content]);

  const loadAssessmentsAndCourses = async () => {
    const [assessRes, coursesRes] = await Promise.all([getAdminAssessments(), getAdminCourses()]);
    setAssessments(assessRes.data || []);
    setCourses(coursesRes.data || []);
  };

  const loadLibrary = async (nextFilters = filters) => {
    const res = await getQuestionLibrary({
      search: nextFilters.search || undefined,
      topic: nextFilters.topic || undefined,
      difficulty: nextFilters.difficulty || undefined,
      assessmentId: nextFilters.assessmentId ? Number(nextFilters.assessmentId) : undefined,
      page: nextFilters.page,
      size: nextFilters.size,
    });
    setLibrary({ ...defaultLibrary, ...(res.data || {}) });
  };

  useEffect(() => {
    Promise.all([loadAssessmentsAndCourses(), loadLibrary()]).catch((err) => {
      console.error(err);
      setError("Failed to load assessments and question library.");
    });
  }, []);

  const toAssessmentPayload = () => ({
    ...assessmentForm,
    duration: Number(assessmentForm.duration),
    courseId: assessmentForm.courseId ? Number(assessmentForm.courseId) : null,
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

  const resetAssessmentWizard = () => {
    setAssessmentForm(initialAssessmentForm);
    setWizardStep(1);
    setWizardInputTab("single");
    setWizardCsvFile(null);
    setDraftQuestions([]);
    setDraftQuestionForm({
      questionText: "",
      optionA: "",
      optionB: "",
      optionC: "",
      optionD: "",
      correctAnswer: "A",
      subject: "",
      topic: "",
      difficulty: "Medium",
    });
  };

  const submitAssessmentWizard = async () => {
    setError("");
    setMessage("");

    const payload = toAssessmentPayload();
    const formData = new FormData();
    formData.append(
      "assessmentJson",
      new Blob([JSON.stringify(payload)], { type: "application/json" })
    );

    if (wizardInputTab === "single" && draftQuestions.length) {
      const csvText = buildWizardCsv(draftQuestions);
      const csvFile = new File([csvText], "assessment_questions.csv", { type: "text/csv" });
      formData.append("file", csvFile);
    } else if (wizardInputTab === "csv" && wizardCsvFile) {
      formData.append("file", wizardCsvFile);
    }

    try {
      const res = await createAssessmentWithQuestions(formData);
      const title = res.data?.assessment?.title || payload.title;
      const questionCount = res.data?.questionCount ?? 0;
      setMessage(`${title} saved with ${questionCount} questions.`);

      resetAssessmentWizard();
      await Promise.all([loadAssessmentsAndCourses(), loadLibrary({ ...filters, page: 0 })]);
    } catch (submitError) {
      setError(submitError.response?.data?.error || "Failed to save assessment.");
    }
  };

  const addDraftQuestion = (e) => {
    e.preventDefault();
    setError("");

    const requiredValues = [
      draftQuestionForm.questionText,
      draftQuestionForm.optionA,
      draftQuestionForm.optionB,
      draftQuestionForm.optionC,
      draftQuestionForm.optionD,
    ];
    if (requiredValues.some((value) => !String(value || "").trim())) {
      setError("Question text and all four options are required.");
      return;
    }

    setDraftQuestions((prev) => [...prev, { ...draftQuestionForm }]);
    setDraftQuestionForm((prev) => ({
      ...prev,
      questionText: "",
      optionA: "",
      optionB: "",
      optionC: "",
      optionD: "",
    }));
  };

  const removeDraftQuestion = (index) => {
    setDraftQuestions((prev) => prev.filter((_, idx) => idx !== index));
  };

  const submitQuestion = async (e) => {
    e.preventDefault();
    setError("");
    setMessage("");

    await createQuestion({
      ...questionForm,
      assessmentId: Number(questionForm.assessmentId),
    });

    setMessage("Question created.");
    setQuestionForm((prev) => ({
      ...prev,
      questionText: "",
      optionA: "",
      optionB: "",
      optionC: "",
      optionD: "",
    }));
    await loadLibrary({ ...filters, page: 0 });
  };

  const uploadBulk = async () => {
    if (!bulkFile) return;
    setError("");
    setMessage("");

    try {
      const res = await bulkUploadQuestions(bulkFile);
      setMessage(res.data?.message || "Bulk upload completed.");
      setBulkFile(null);
      await loadLibrary({ ...filters, page: 0 });
    } catch (uploadError) {
      setError(uploadError.response?.data?.error || "Bulk upload failed.");
    }
  };

  const onFilterChange = async (key, value) => {
    const next = { ...filters, [key]: value, page: 0 };
    setFilters(next);
    await loadLibrary(next);
  };

  const goToPage = async (page) => {
    const next = { ...filters, page };
    setFilters(next);
    await loadLibrary(next);
  };

  const toggleSelect = (id) => {
    setSelectedIds((prev) => (prev.includes(id) ? prev.filter((value) => value !== id) : [...prev, id]));
  };

  const selectAllOnPage = () => {
    const ids = (library.content || []).map((item) => item.id);
    setSelectedIds(ids);
  };

  const clearSelection = () => setSelectedIds([]);

  const handleBulkDelete = async () => {
    if (!selectedIds.length) return;
    if (!window.confirm(`Delete ${selectedIds.length} selected questions?`)) return;

    await deleteQuestionsBulk(selectedIds);
    setMessage(`Deleted ${selectedIds.length} questions.`);
    setSelectedIds([]);
    await loadLibrary(filters);
  };

  const handleDeleteRow = async (id) => {
    if (!window.confirm("Delete this question?")) return;
    await deleteQuestion(id);
    await loadLibrary(filters);
  };

  const startInlineEdit = (question) => {
    setEditingQuestionId(question.id);
    setEditForm({
      questionText: question.questionText || "",
      topic: question.topic || "",
      difficulty: question.difficulty || "",
      correctAnswer: question.correctAnswer || "A",
    });
  };

  const saveInlineEdit = async (question) => {
    await updateQuestion(question.id, {
      ...question,
      ...editForm,
      assessmentId: question.assessment?.id,
    });
    setEditingQuestionId(null);
    await loadLibrary(filters);
  };

  const canGoStepTwo = assessmentForm.title.trim() && Number(assessmentForm.duration) > 0;

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Assessments</div>
          <h1 className="page-title">Assessment Builder and Question Bank</h1>
          <p className="page-subtitle">
            Create targeted assessments, map them to courses, and manage questions with search, filters, and bulk tools.
          </p>
        </div>
      </section>

      <section className="surface-panel">
        <div className="card-actions">
          <button className={`btn ${tab === "assessment" ? "btn-primary" : "btn-secondary"} btn-sm`} onClick={() => setTab("assessment")}>Assessment Builder</button>
          <button className={`btn ${tab === "single" ? "btn-primary" : "btn-secondary"} btn-sm`} onClick={() => setTab("single")}>Add Single</button>
          <button className={`btn ${tab === "bulk" ? "btn-primary" : "btn-secondary"} btn-sm`} onClick={() => setTab("bulk")}>Bulk CSV Upload</button>
          <button className={`btn ${tab === "library" ? "btn-primary" : "btn-secondary"} btn-sm`} onClick={() => setTab("library")}>Question Bank</button>
        </div>
      </section>

      {tab === "assessment" && (
        <section className="surface-panel">
          {wizardStep === 1 ? (
            <>
              <h3>Step 1: Assessment Metadata</h3>
              <form>
                <div className="form-grid mt-2">
                  <div className="form-group">
                    <label htmlFor="title">Title</label>
                    <input id="title" value={assessmentForm.title} onChange={(e) => setAssessmentForm((prev) => ({ ...prev, title: e.target.value }))} required />
                  </div>
                  <div className="form-group">
                    <label htmlFor="course">Course</label>
                    <select id="course" value={assessmentForm.courseId} onChange={(e) => setAssessmentForm((prev) => ({ ...prev, courseId: e.target.value }))}>
                      <option value="">Standalone (no course link)</option>
                      {courses.map((course) => (
                        <option key={course.id} value={course.id}>{course.title}</option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group">
                    <label htmlFor="type">Type</label>
                    <select id="type" value={assessmentForm.assessmentType} onChange={(e) => setAssessmentForm((prev) => ({ ...prev, assessmentType: e.target.value }))}>
                      {assessmentTypes.map((type) => (
                        <option key={type} value={type}>{type}</option>
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
                    <label htmlFor="branches">Target Branches</label>
                    <input id="branches" value={assessmentForm.targetBranches} onChange={(e) => setAssessmentForm((prev) => ({ ...prev, targetBranches: e.target.value }))} placeholder="CSE,IT,DS" />
                  </div>
                  <div className="form-group">
                    <label htmlFor="batches">Target Batch Years</label>
                    <input id="batches" value={assessmentForm.targetBatchYears} onChange={(e) => setAssessmentForm((prev) => ({ ...prev, targetBatchYears: e.target.value }))} placeholder="2024,2025" />
                  </div>
                  <div className="form-group">
                    <label htmlFor="sections">Target Sections</label>
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
                  <label className="btn btn-secondary btn-sm" style={{ cursor: "pointer" }}>
                    <input
                      type="checkbox"
                      checked={assessmentForm.reviewAfterClose}
                      onChange={(e) => setAssessmentForm((prev) => ({ ...prev, reviewAfterClose: e.target.checked }))}
                      style={{ marginRight: "0.5rem" }}
                    />
                    Review After Close
                  </label>
                  <button
                    type="button"
                    className="btn btn-primary btn-sm"
                    onClick={() => setWizardStep(2)}
                    disabled={!canGoStepTwo}
                  >
                    Continue to Step 2
                  </button>
                </div>
              </form>
            </>
          ) : (
            <>
              <h3>Step 2: Add Questions</h3>
              <p className="section-subtitle mt-1">
                CSV columns: <strong>questionText, optionA, optionB, optionC, optionD, correctAnswer</strong>
                <br />
                Optional columns: <strong>subject, topic, difficulty</strong>
              </p>

              <div className="card-actions mt-2">
                <button
                  className={`btn ${wizardInputTab === "single" ? "btn-primary" : "btn-secondary"} btn-sm`}
                  onClick={() => setWizardInputTab("single")}
                >
                  Add Single Question
                </button>
                <button
                  className={`btn ${wizardInputTab === "csv" ? "btn-primary" : "btn-secondary"} btn-sm`}
                  onClick={() => setWizardInputTab("csv")}
                >
                  Upload CSV
                </button>
              </div>

              {wizardInputTab === "single" ? (
                <>
                  <form onSubmit={addDraftQuestion} className="form-grid mt-2">
                    <div className="form-group form-group-full">
                      <label htmlFor="draftQuestionText">Question Text</label>
                      <textarea id="draftQuestionText" value={draftQuestionForm.questionText} onChange={(e) => setDraftQuestionForm((prev) => ({ ...prev, questionText: e.target.value }))} required />
                    </div>
                    <div className="form-group">
                      <label htmlFor="draftCorrect">Correct Answer</label>
                      <select id="draftCorrect" value={draftQuestionForm.correctAnswer} onChange={(e) => setDraftQuestionForm((prev) => ({ ...prev, correctAnswer: e.target.value }))}>
                        {"ABCD".split("").map((value) => (
                          <option key={value} value={value}>{value}</option>
                        ))}
                      </select>
                    </div>
                    <div className="form-group">
                      <label htmlFor="draftSubject">Subject</label>
                      <input id="draftSubject" value={draftQuestionForm.subject} onChange={(e) => setDraftQuestionForm((prev) => ({ ...prev, subject: e.target.value }))} />
                    </div>
                    <div className="form-group">
                      <label htmlFor="draftTopic">Topic</label>
                      <input id="draftTopic" value={draftQuestionForm.topic} onChange={(e) => setDraftQuestionForm((prev) => ({ ...prev, topic: e.target.value }))} />
                    </div>
                    <div className="form-group">
                      <label htmlFor="draftDifficulty">Difficulty</label>
                      <select id="draftDifficulty" value={draftQuestionForm.difficulty} onChange={(e) => setDraftQuestionForm((prev) => ({ ...prev, difficulty: e.target.value }))}>
                        {["Easy", "Medium", "Hard"].map((value) => (
                          <option key={value} value={value}>{value}</option>
                        ))}
                      </select>
                    </div>
                    {"ABCD".split("").map((option) => (
                      <div key={option} className="form-group">
                        <label htmlFor={`draftOption${option}`}>Option {option}</label>
                        <input
                          id={`draftOption${option}`}
                          value={draftQuestionForm[`option${option}`]}
                          onChange={(e) => setDraftQuestionForm((prev) => ({ ...prev, [`option${option}`]: e.target.value }))}
                          required
                        />
                      </div>
                    ))}
                    <div className="card-actions form-group-full">
                      <button className="btn btn-secondary btn-sm" type="submit">Add to Assessment Draft</button>
                    </div>
                  </form>

                  {!draftQuestions.length ? (
                    <div className="empty-state mt-2">
                      <p>No draft questions added yet.</p>
                    </div>
                  ) : (
                    <div className="card-grid mt-2">
                      {draftQuestions.map((question, index) => (
                        <article key={`${question.questionText}-${index}`} className="card">
                          <h3>Q{index + 1}</h3>
                          <p>{question.questionText}</p>
                          <p style={{ color: "var(--text-muted)" }}>Correct: {question.correctAnswer}</p>
                          <div className="card-actions">
                            <button className="btn btn-danger btn-sm" onClick={() => removeDraftQuestion(index)}>Remove</button>
                          </div>
                        </article>
                      ))}
                    </div>
                  )}
                </>
              ) : (
                <div className="form-group mt-2">
                  <label htmlFor="wizard-bulk-file">Upload CSV</label>
                  <input
                    id="wizard-bulk-file"
                    type="file"
                    accept=".csv"
                    onChange={(e) => setWizardCsvFile(e.target.files?.[0] || null)}
                  />
                </div>
              )}

              <div className="card-actions mt-2">
                <button type="button" className="btn btn-secondary btn-sm" onClick={() => setWizardStep(1)}>Back</button>
                <button type="button" className="btn btn-primary btn-sm" onClick={submitAssessmentWizard}>Save Assessment</button>
              </div>
            </>
          )}

          <div className="card-grid mt-2">
            {assessments.map((assessment) => (
              <article key={assessment.id} className="card">
                <h3>{assessment.title}</h3>
                <p>{assessment.assessmentType}</p>
                <p>{assessment.course?.title || "No course"}</p>
                <p>{assessment.startTime ? new Date(assessment.startTime).toLocaleString() : "N/A"}</p>
                <div className="card-actions">
                  <button className="btn btn-danger btn-sm" onClick={() => deleteAssessment(assessment.id).then(loadAssessmentsAndCourses)}>
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
                      {assessment.title} ({assessmentCounts[assessment.id] || 0})
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label htmlFor="correct">Correct Answer</label>
                <select id="correct" value={questionForm.correctAnswer} onChange={(e) => setQuestionForm((prev) => ({ ...prev, correctAnswer: e.target.value }))}>
                  {"ABCD".split("").map((value) => (
                    <option key={value} value={value}>{value}</option>
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
                    <option key={value} value={value}>{value}</option>
                  ))}
                </select>
              </div>
              {"ABCD".split("").map((option) => (
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
              <button className="btn btn-primary btn-sm" type="submit">Add Question</button>
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
              <button className="btn btn-primary btn-sm" onClick={uploadBulk}>Upload {bulkFile.name}</button>
            </div>
          )}
        </section>
      )}

      {tab === "library" && (
        <section className="surface-panel">
          <div className="form-grid" style={{ marginBottom: "1rem" }}>
            <div className="form-group">
              <label htmlFor="search">Search</label>
              <input id="search" value={filters.search} onChange={(e) => onFilterChange("search", e.target.value)} placeholder="Text, topic, subject" />
            </div>
            <div className="form-group">
              <label htmlFor="filter-topic">Topic</label>
              <select id="filter-topic" value={filters.topic} onChange={(e) => onFilterChange("topic", e.target.value)}>
                <option value="">All topics</option>
                {topicOptions.map((topic) => (
                  <option key={topic} value={topic}>{topic}</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label htmlFor="filter-difficulty">Difficulty</label>
              <select id="filter-difficulty" value={filters.difficulty} onChange={(e) => onFilterChange("difficulty", e.target.value)}>
                <option value="">All</option>
                <option value="Easy">Easy</option>
                <option value="Medium">Medium</option>
                <option value="Hard">Hard</option>
              </select>
            </div>
            <div className="form-group">
              <label htmlFor="filter-assessment">Assessment</label>
              <select id="filter-assessment" value={filters.assessmentId} onChange={(e) => onFilterChange("assessmentId", e.target.value)}>
                <option value="">All assessments</option>
                {assessments.map((assessment) => (
                  <option key={assessment.id} value={assessment.id}>{assessment.title}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="card-actions" style={{ marginTop: 0 }}>
            <button className="btn btn-secondary btn-sm" onClick={selectAllOnPage}>Select All Page</button>
            <button className="btn btn-secondary btn-sm" onClick={clearSelection}>Clear Selection</button>
            <button className="btn btn-danger btn-sm" disabled={!selectedIds.length} onClick={handleBulkDelete}>Bulk Delete ({selectedIds.length})</button>
          </div>

          {!library.content?.length ? (
            <div className="empty-state">
              <p>No matching questions found.</p>
            </div>
          ) : (
            <div style={{ overflowX: "auto" }}>
              <table className="data-table">
                <thead>
                  <tr>
                    <th></th>
                    <th>Question</th>
                    <th>Topic</th>
                    <th>Difficulty</th>
                    <th>Assessment</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {library.content.map((question) => {
                    const isEditing = editingQuestionId === question.id;
                    return (
                      <tr key={question.id}>
                        <td>
                          <input type="checkbox" checked={selectedIds.includes(question.id)} onChange={() => toggleSelect(question.id)} />
                        </td>
                        <td style={{ minWidth: "320px" }}>
                          {isEditing ? (
                            <textarea value={editForm.questionText} onChange={(e) => setEditForm((prev) => ({ ...prev, questionText: e.target.value }))} />
                          ) : (
                            question.questionText
                          )}
                        </td>
                        <td>
                          {isEditing ? (
                            <input value={editForm.topic} onChange={(e) => setEditForm((prev) => ({ ...prev, topic: e.target.value }))} />
                          ) : (
                            question.topic || "-"
                          )}
                        </td>
                        <td>
                          {isEditing ? (
                            <select value={editForm.difficulty} onChange={(e) => setEditForm((prev) => ({ ...prev, difficulty: e.target.value }))}>
                              <option value="Easy">Easy</option>
                              <option value="Medium">Medium</option>
                              <option value="Hard">Hard</option>
                            </select>
                          ) : (
                            question.difficulty || "-"
                          )}
                        </td>
                        <td>{question.assessment?.title || "-"}</td>
                        <td>
                          <div className="card-actions" style={{ marginTop: 0 }}>
                            {!isEditing ? (
                              <button className="btn btn-secondary btn-sm" onClick={() => startInlineEdit(question)}>Edit</button>
                            ) : (
                              <button className="btn btn-primary btn-sm" onClick={() => saveInlineEdit(question)}>Save</button>
                            )}
                            {isEditing && (
                              <button className="btn btn-secondary btn-sm" onClick={() => setEditingQuestionId(null)}>Cancel</button>
                            )}
                            <button className="btn btn-danger btn-sm" onClick={() => handleDeleteRow(question.id)}>Delete</button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}

          <div className="card-actions">
            <button className="btn btn-secondary btn-sm" disabled={filters.page <= 0} onClick={() => goToPage(filters.page - 1)}>Previous</button>
            <span className="badge badge-neutral">Page {filters.page + 1} of {Math.max(library.totalPages, 1)}</span>
            <button className="btn btn-secondary btn-sm" disabled={filters.page + 1 >= library.totalPages} onClick={() => goToPage(filters.page + 1)}>Next</button>
            <span className="badge badge-primary">Total: {library.totalElements}</span>
          </div>
        </section>
      )}

      {message && <div className="alert alert-success">{message}</div>}
      {error && <div className="alert alert-error">{error}</div>}
    </Layout>
  );
}
