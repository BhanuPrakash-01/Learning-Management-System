import { useEffect, useState } from "react";
import Layout from "../../components/Layout";
import { createCourse, deleteCourse, getAdminCourses, updateCourse } from "../../services/adminService";

export default function Courses() {
  const [courses, setCourses] = useState([]);
  const [form, setForm] = useState({ title: "", description: "", instructor: "", duration: "" });
  const [editId, setEditId] = useState(null);

  useEffect(() => {
    loadCourses();
  }, []);

  const loadCourses = async () => {
    try {
      const res = await getAdminCourses();
      setCourses(res.data);
    } catch (error) {
      console.error("Failed to load courses:", error);
    }
  };

  const resetForm = () => {
    setForm({ title: "", description: "", instructor: "", duration: "" });
    setEditId(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      const data = { ...form, duration: parseInt(form.duration, 10) || 0 };

      if (editId) {
        await updateCourse(editId, data);
      } else {
        await createCourse(data);
      }

      resetForm();
      loadCourses();
    } catch (error) {
      alert(`Error: ${error.response?.data?.error || error.message}`);
    }
  };

  const handleEdit = (course) => {
    setForm({
      title: course.title,
      description: course.description || "",
      instructor: course.instructor || "",
      duration: course.duration?.toString() || "",
    });
    setEditId(course.id);
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Are you sure you want to delete this course?")) return;

    try {
      await deleteCourse(id);
      loadCourses();
    } catch (error) {
      alert(`Error deleting course: ${error.response?.data || error.message}`);
    }
  };

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Course management</div>
          <h1 className="page-title">Create and maintain the course catalog.</h1>
          <p className="page-subtitle">
            Update titles, instructors, descriptions, and estimated duration from one structured panel.
          </p>
        </div>
        <div className="hero-aside">
          <div className="hero-badge">
            <span className="hero-badge-label">Total courses</span>
            <span className="hero-badge-value">{courses.length}</span>
          </div>
        </div>
      </section>

      <section className="form-section">
        <h3>{editId ? "Edit course" : "Create a course"}</h3>
        <p className="form-intro">Use the form below to manage the live course catalog.</p>

        <form onSubmit={handleSubmit}>
          <div className="form-grid">
            <div className="form-group">
              <label htmlFor="course-title">Title</label>
              <input
                id="course-title"
                placeholder="Course title"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="course-instructor">Instructor</label>
              <input
                id="course-instructor"
                placeholder="Instructor name"
                value={form.instructor}
                onChange={(e) => setForm({ ...form, instructor: e.target.value })}
              />
            </div>

            <div className="form-group">
              <label htmlFor="course-duration">Duration (hours)</label>
              <input
                id="course-duration"
                type="number"
                placeholder="10"
                value={form.duration}
                onChange={(e) => setForm({ ...form, duration: e.target.value })}
              />
            </div>

            <div className="form-group form-group-full">
              <label htmlFor="course-description">Description</label>
              <textarea
                id="course-description"
                placeholder="Course description"
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </div>
          </div>

          <div className="card-actions">
            <button type="submit" className="btn btn-primary">
              {editId ? "Update Course" : "Create Course"}
            </button>
            {editId && (
              <button type="button" className="btn btn-secondary" onClick={resetForm}>
                Cancel
              </button>
            )}
          </div>
        </form>
      </section>

      {courses.length === 0 ? (
        <div className="empty-state">
          <p>No courses yet. Create one above to get started.</p>
        </div>
      ) : (
        <div className="card-grid">
          {courses.map((course) => (
            <article key={course.id} className="card">
              <h3>{course.title}</h3>
              <p>{course.description || "No description available."}</p>

              <div className="card-meta">
                <div className="meta-item">
                  <span>Instructor</span>
                  <strong>{course.instructor || "N/A"}</strong>
                </div>
                <div className="meta-item">
                  <span>Duration</span>
                  <strong>{course.duration} hours</strong>
                </div>
              </div>

              <div className="card-actions">
                <button className="btn btn-secondary btn-sm" onClick={() => handleEdit(course)}>
                  Edit
                </button>
                <button className="btn btn-danger btn-sm" onClick={() => handleDelete(course.id)}>
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
