import { useEffect, useMemo, useState } from "react";
import Layout from "../../components/Layout";
import {
  bulkUploadPracticeQuestions,
  createPracticeCategory,
  createPracticeTopicWithQuestions,
  getPracticeCsvTemplate,
  getPracticeCategories,
  togglePracticeTopicActive,
} from "../../services/adminService";

export default function PracticeManager() {
  const [categories, setCategories] = useState([]);
  const [categoryName, setCategoryName] = useState("");
  const [topicForm, setTopicForm] = useState({
    categoryId: "",
    name: "",
    description: "",
    icon: "",
  });
  const [topicCsvFile, setTopicCsvFile] = useState(null);
  const [selectedTopicId, setSelectedTopicId] = useState("");
  const [bulkFile, setBulkFile] = useState(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const load = async () => {
    const res = await getPracticeCategories();
    setCategories(res.data || []);
  };

  useEffect(() => {
    load().catch((err) => {
      console.error(err);
      setError("Failed to load practice categories.");
    });
  }, []);

  const categoryNameMap = useMemo(() => {
    const map = {};
    (categories || []).forEach((category) => {
      map[category.id] = category.name;
    });
    return map;
  }, [categories]);

  const topics = useMemo(() => {
    const rows = [];
    (categories || []).forEach((category) => {
      (category.topics || []).forEach((topic) => {
        rows.push(topic);
      });
    });
    return rows.sort((a, b) => (a.name || "").localeCompare(b.name || ""));
  }, [categories]);

  const addCategory = async (e) => {
    e.preventDefault();
    setError("");
    setMessage("");
    try {
      await createPracticeCategory({ name: categoryName });
      setCategoryName("");
      setMessage("Category added.");
      await load();
    } catch (err) {
      setError(err.response?.data?.error || "Failed to create category.");
    }
  };

  const addTopic = async (e) => {
    e.preventDefault();
    setError("");
    setMessage("");

    try {
      const payload = {
        ...topicForm,
        categoryId: Number(topicForm.categoryId),
      };

      const formData = new FormData();
      formData.append(
        "topicJson",
        new Blob([JSON.stringify(payload)], { type: "application/json" })
      );
      if (topicCsvFile) {
        formData.append("file", topicCsvFile);
      }

      const res = await createPracticeTopicWithQuestions(formData);
      const createdName = res.data?.topic?.name || payload.name;
      const count = res.data?.questionCount ?? 0;

      setTopicForm({ categoryId: "", name: "", description: "", icon: "" });
      setTopicCsvFile(null);
      setMessage(`${createdName} created with ${count} questions.`);
      await load();
    } catch (err) {
      setError(err.response?.data?.error || "Failed to create topic.");
    }
  };

  const toggleActive = async (topic) => {
    setError("");
    setMessage("");
    try {
      const res = await togglePracticeTopicActive(topic.id);
      const isActive = Boolean(res.data?.active);
      setMessage(`${topic.name} is now ${isActive ? "active" : "inactive"}.`);
      await load();
    } catch (err) {
      setError(err.response?.data?.error || "Failed to update topic status.");
    }
  };

  const uploadCsv = async () => {
    if (!bulkFile || !selectedTopicId) return;
    setError("");
    setMessage("");

    try {
      const res = await bulkUploadPracticeQuestions(bulkFile, Number(selectedTopicId));
      setMessage(res.data?.message || "Practice questions uploaded.");
      setBulkFile(null);
      await load();
    } catch (err) {
      setError(err.response?.data?.error || "Bulk upload failed.");
    }
  };

  const downloadTemplate = async () => {
    const res = await getPracticeCsvTemplate();
    const url = window.URL.createObjectURL(new Blob([res.data], { type: "text/csv" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = "practice_questions_template.csv";
    link.click();
    window.URL.revokeObjectURL(url);
  };

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Practice Manager</div>
          <h1 className="page-title">Manage aptitude, technical, and coding practice sets</h1>
          <p className="page-subtitle">
            Create topics with optional inline CSV upload, and control which topics are visible to students.
          </p>
        </div>
      </section>

      <section className="surface-panel">
        <h3>Add Category</h3>
        <form onSubmit={addCategory} className="card-actions">
          <input
            value={categoryName}
            onChange={(e) => setCategoryName(e.target.value)}
            placeholder="Quantitative Aptitude"
            required
          />
          <button className="btn btn-primary btn-sm" type="submit">
            Add
          </button>
        </form>
      </section>

      <section className="surface-panel">
        <h3>Create Topic</h3>
        <p className="section-subtitle mt-1">
          Optional CSV columns: <strong>questionText, optionsJson, correctAnswer, explanation, difficulty, type</strong>
        </p>
        <form onSubmit={addTopic} className="form-grid mt-2">
          <div className="form-group">
            <label htmlFor="categoryId">Category</label>
            <select
              id="categoryId"
              value={topicForm.categoryId}
              onChange={(e) => setTopicForm((prev) => ({ ...prev, categoryId: e.target.value }))}
              required
            >
              <option value="">Select category</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="topicName">Topic Name</label>
            <input
              id="topicName"
              value={topicForm.name}
              onChange={(e) => setTopicForm((prev) => ({ ...prev, name: e.target.value }))}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="topicDesc">Description</label>
            <input
              id="topicDesc"
              value={topicForm.description}
              onChange={(e) => setTopicForm((prev) => ({ ...prev, description: e.target.value }))}
            />
          </div>

          <div className="form-group">
            <label htmlFor="topicIcon">Icon</label>
            <input
              id="topicIcon"
              value={topicForm.icon}
              onChange={(e) => setTopicForm((prev) => ({ ...prev, icon: e.target.value }))}
            />
          </div>

          <div className="form-group form-group-full">
            <label htmlFor="topicCsv">Upload practice questions (optional)</label>
            <input
              id="topicCsv"
              type="file"
              accept=".csv"
              onChange={(e) => setTopicCsvFile(e.target.files?.[0] || null)}
            />
          </div>

          <div className="card-actions form-group-full">
            <button className="btn btn-primary btn-sm" type="submit">
              Create Topic
            </button>
          </div>
        </form>
      </section>

      <section className="surface-panel">
        <div className="section-heading">
          <div>
            <h2 className="section-title">Topics</h2>
            <p className="section-subtitle">Toggle topic visibility for students.</p>
          </div>
        </div>

        {!topics.length ? (
          <div className="empty-state mt-2">
            <p>No topics created yet.</p>
          </div>
        ) : (
          <div className="card-grid mt-2">
            {topics.map((topic) => (
              <article key={topic.id} className="card">
                <h3>{topic.name}</h3>
                <p>{topic.description || "No description available."}</p>
                <div className="card-meta">
                  <div className="meta-item">
                    <span>Category</span>
                    <strong>{categoryNameMap[topic.categoryId] || "Unknown"}</strong>
                  </div>
                  <div className="meta-item">
                    <span>Questions</span>
                    <strong>{topic.questionCount || 0}</strong>
                  </div>
                  <div className="meta-item">
                    <span>Status</span>
                    <strong>{topic.active ? "Active" : "Inactive"}</strong>
                  </div>
                </div>
                <div className="card-actions">
                  <button className="btn btn-secondary btn-sm" onClick={() => toggleActive(topic)}>
                    {topic.active ? "Set Inactive" : "Set Active"}
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      <section className="surface-panel">
        <h3>Bulk Upload Practice Questions</h3>
        <p className="section-subtitle">Pick a topic first, then upload a CSV without a topicId column.</p>

        <div className="card-actions mt-2">
          <button className="btn btn-secondary btn-sm" onClick={downloadTemplate}>
            Download CSV Template
          </button>
        </div>

        <div className="form-grid mt-2">
          <div className="form-group">
            <label htmlFor="bulkTopicId">Target Topic</label>
            <select
              id="bulkTopicId"
              value={selectedTopicId}
              onChange={(e) => setSelectedTopicId(e.target.value)}
            >
              <option value="">Select topic</option>
              {topics.map((topic) => (
                <option key={topic.id} value={topic.id}>
                  {topic.name}
                </option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label htmlFor="bulkPracticeCsv">CSV File</label>
            <input
              id="bulkPracticeCsv"
              type="file"
              accept=".csv"
              onChange={(e) => setBulkFile(e.target.files?.[0] || null)}
            />
          </div>
        </div>

        <div className="card-actions mt-2">
          <button className="btn btn-primary btn-sm" onClick={uploadCsv} disabled={!bulkFile || !selectedTopicId}>
            Upload CSV
          </button>
        </div>
      </section>

      {message && <div className="alert alert-success">{message}</div>}
      {error && <div className="alert alert-error">{error}</div>}
    </Layout>
  );
}
