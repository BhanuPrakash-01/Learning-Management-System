import { useEffect, useState } from "react";
import Layout from "../../components/Layout";
import {
  bulkUploadPracticeQuestions,
  createPracticeCategory,
  createPracticeTopic,
  getPracticeCategories,
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
  const [file, setFile] = useState(null);
  const [message, setMessage] = useState("");

  const load = async () => {
    const res = await getPracticeCategories();
    setCategories(res.data || []);
  };

  useEffect(() => {
    load().catch((err) => console.error(err));
  }, []);

  const addCategory = async (e) => {
    e.preventDefault();
    await createPracticeCategory({ name: categoryName });
    setCategoryName("");
    setMessage("Category added.");
    load();
  };

  const addTopic = async (e) => {
    e.preventDefault();
    await createPracticeTopic({
      ...topicForm,
      categoryId: Number(topicForm.categoryId),
    });
    setTopicForm({ categoryId: "", name: "", description: "", icon: "" });
    setMessage("Topic created.");
    load();
  };

  const uploadCsv = async () => {
    if (!file) return;
    const res = await bulkUploadPracticeQuestions(file);
    setMessage(res.data?.message || "Practice questions uploaded.");
    setFile(null);
  };

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Practice Manager</div>
          <h1 className="page-title">Manage aptitude, technical, and coding practice sets</h1>
          <p className="page-subtitle">
            Build categories and topics, then bulk-upload question pools for student practice and topic tests.
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
          <div className="card-actions form-group-full">
            <button className="btn btn-primary btn-sm" type="submit">
              Create Topic
            </button>
          </div>
        </form>
      </section>

      <section className="surface-panel">
        <h3>Bulk Upload Practice Questions</h3>
        <p className="section-subtitle">CSV format: topicId,questionText,optionsJson,correctAnswer,explanation,difficulty,type</p>
        <input type="file" accept=".csv" onChange={(e) => setFile(e.target.files?.[0] || null)} />
        <div className="card-actions mt-2">
          <button className="btn btn-primary btn-sm" onClick={uploadCsv} disabled={!file}>
            Upload CSV
          </button>
        </div>
      </section>

      <section className="surface-panel">
        <h3>Existing Categories</h3>
        <div className="card-grid mt-2">
          {categories.map((category) => (
            <article key={category.id} className="card">
              <h3>{category.name}</h3>
            </article>
          ))}
        </div>
      </section>

      {message && <div className="alert alert-success">{message}</div>}
    </Layout>
  );
}
