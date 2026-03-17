import { useEffect, useState } from "react";
import Layout from "../../components/Layout";
import {
  createAssessment,
  deleteAssessment,
  getAdminAssessments,
  getAdminCourses,
  updateAssessment,
} from "../../services/adminService";

export default function AdminAssessments() {
  const [assessments, setAssessments] = useState([]);
  const [courses, setCourses] = useState([]);
  const [form, setForm] = useState({ title: "", description: "", duration: "", courseId: "" });
  const [editId, setEditId] = useState(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [assessRes, coursesRes] = await Promise.all([getAdminAssessments(), getAdminCourses()]);
      setAssessments(assessRes.data);
      setCourses(coursesRes.data);
    } catch (error) {
      console.error("Failed to load data:", error);
    }
  };

  const resetForm = () => {
    setForm({ title: "", description: "", duration: "", courseId: "" });
    setEditId(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!form.courseId) {
      alert("Please select a course");
      return;
    }

    try {
      const data = {
        title: form.title,
        description: form.description,
        duration: parseInt(form.duration, 10) || 30,
        courseId: parseInt(form.courseId, 10),
      };

      if (editId) {
        await updateAssessment(editId, data);
      } else {
        await createAssessment(data);
      }

      resetForm();
      loadData();
    } catch (error) {
      alert(`Error: ${error.response?.data?.error || error.message}`);
    }
  };

  const handleEdit = (assessment) => {
    setForm({
      title: assessment.title,
      description: assessment.description || "",
      duration: assessment.duration?.toString() || "",
      courseId: assessment.course?.id?.toString() || "",
    });
    setEditId(assessment.id);
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Delete this assessment?")) return;

    try {
      await deleteAssessment(id);
      loadData();
    } catch (error) {
      alert(`Error deleting: ${error.response?.data || error.message}`);
    }
  };

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Assessment center</div>
          <h1 className="page-title">Create timed assessments linked to courses.</h1>
          <p className="page-subtitle">
            Configure the test catalog with course context, descriptions, and duration settings.
          </p>
        </div>
        <div className="hero-aside">
          <div className="hero-badge">
            <span className="hero-badge-label">Assessments</span>
            <span className="hero-badge-value">{assessments.length}</span>
          </div>
        </div>
      </section>

      <section className="form-section">
        <h3>{editId ? "Edit assessment" : "Create an assessment"}</h3>
        <p className="form-intro">Link each assessment to a course before saving it.</p>

        <form onSubmit={handleSubmit}>
          <div className="form-grid">
            <div className="form-group">
              <label htmlFor="assessment-title">Title</label>
              <input
                id="assessment-title"
                placeholder="Assessment title"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="assessment-course">Course</label>
              <select
                id="assessment-course"
                value={form.courseId}
                onChange={(e) => setForm({ ...form, courseId: e.target.value })}
                required
              >
                <option value="">Select course</option>
                {courses.map((course) => (
                  <option key={course.id} value={course.id}>
                    {course.title}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="assessment-duration">Duration (minutes)</label>
              <input
                id="assessment-duration"
                type="number"
                placeholder="30"
                value={form.duration}
                onChange={(e) => setForm({ ...form, duration: e.target.value })}
              />
            </div>

            <div className="form-group form-group-full">
              <label htmlFor="assessment-description">Description</label>
              <textarea
                id="assessment-description"
                placeholder="Assessment description"
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </div>
          </div>

          <div className="card-actions">
            <button type="submit" className="btn btn-primary">
              {editId ? "Update Assessment" : "Create Assessment"}
            </button>
            {editId && (
              <button type="button" className="btn btn-secondary" onClick={resetForm}>
                Cancel
              </button>
            )}
          </div>
        </form>
      </section>

      {assessments.length === 0 ? (
        <div className="empty-state">
          <p>No assessments yet. Create one above.</p>
        </div>
      ) : (
        <div className="card-grid">
          {assessments.map((assessment) => (
            <article key={assessment.id} className="card">
              <div className="list-card-top">
                <div>
                  <h3>{assessment.title}</h3>
                  <p>{assessment.description || "No description available."}</p>
                </div>
                <span className="badge badge-primary">{assessment.course?.title || "N/A"}</span>
              </div>

              <div className="card-meta">
                <div className="meta-item">
                  <span>Duration</span>
                  <strong>{assessment.duration} minutes</strong>
                </div>
              </div>

              <div className="card-actions">
                <button className="btn btn-secondary btn-sm" onClick={() => handleEdit(assessment)}>
                  Edit
                </button>
                <button className="btn btn-danger btn-sm" onClick={() => handleDelete(assessment.id)}>
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
