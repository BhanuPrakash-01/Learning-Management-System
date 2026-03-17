import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../api/axios";
import Layout from "../../components/Layout";
import { enrollCourse, getCourses } from "../../services/courseService";

export default function StudentDashboard() {
  const [courses, setCourses] = useState([]);
  const [enrollments, setEnrollments] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [coursesRes, enrollRes] = await Promise.all([getCourses(), api.get("/student/enrollments")]);
      setCourses(coursesRes.data);
      setEnrollments(enrollRes.data);
    } catch (error) {
      console.error(error);
    }
  };

  const handleEnroll = async (courseId) => {
    try {
      await enrollCourse(courseId);
      loadData();
    } catch (error) {
      alert(error.response?.data?.message || error.response?.data || "Error enrolling");
    }
  };

  const enrolledCourseIds = enrollments.map((entry) => entry.course?.id);

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Student workspace</div>
          <h1 className="page-title">Track your learning and start assessments on time.</h1>
          <p className="page-subtitle">
            Browse active courses, keep your enrollments organized, and jump directly into the
            latest assessments from a cleaner Figma-inspired dashboard.
          </p>
        </div>

        <div className="hero-aside">
          <div className="hero-badge">
            <span className="hero-badge-label">Available courses</span>
            <span className="hero-badge-value">{courses.length}</span>
          </div>
          <div className="hero-badge">
            <span className="hero-badge-label">My enrollments</span>
            <span className="hero-badge-value">{enrollments.length}</span>
          </div>
        </div>
      </section>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-label">Course Catalog</div>
          <div className="stat-value">{courses.length}</div>
          <div className="stat-note">Programs currently available for enrollment.</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Enrolled Courses</div>
          <div className="stat-value">{enrollments.length}</div>
          <div className="stat-note">Courses already added to your learning plan.</div>
        </div>
        <div className="stat-card interactive" onClick={() => navigate("/student/assessments")}>
          <div className="stat-label">Assessment Center</div>
          <div className="stat-value">Open</div>
          <div className="stat-note">Move to your active and upcoming assessments.</div>
        </div>
      </div>

      {enrollments.length > 0 && (
        <section className="surface-panel">
          <div className="section-heading">
            <div>
              <h2 className="section-title">My enrolled courses</h2>
              <p className="section-subtitle">Quick access to the courses you have already joined.</p>
            </div>
          </div>

          <div className="card-grid mt-2">
            {enrollments.map((entry) => (
              <article key={entry.id} className="card">
                <h3>{entry.course?.title}</h3>
                <p>{entry.course?.description || "No description available."}</p>
                <div className="card-meta">
                  <div className="meta-item">
                    <span>Instructor</span>
                    <strong>{entry.course?.instructor || "N/A"}</strong>
                  </div>
                </div>
                <div className="card-actions">
                  <span className="badge badge-success">Enrolled</span>
                </div>
              </article>
            ))}
          </div>
        </section>
      )}

      <section className="surface-panel">
        <div className="section-heading">
          <div>
            <h2 className="section-title">Available courses</h2>
            <p className="section-subtitle">Use the catalog below to join a course and unlock its assessments.</p>
          </div>
        </div>

        {courses.length === 0 ? (
          <div className="empty-state mt-2">
            <p>No courses available yet.</p>
          </div>
        ) : (
          <div className="card-grid mt-2">
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
                  {enrolledCourseIds.includes(course.id) ? (
                    <span className="badge badge-success">Already enrolled</span>
                  ) : (
                    <button className="btn btn-primary btn-sm" onClick={() => handleEnroll(course.id)}>
                      Enroll Now
                    </button>
                  )}
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </Layout>
  );
}
