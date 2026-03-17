import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Layout from "../../components/Layout";
import { getAdminAssessments, getAdminCourses, getAdminQuestions } from "../../services/adminService";

export default function AdminDashboard() {
  const [stats, setStats] = useState({ courses: 0, assessments: 0, questions: 0 });
  const navigate = useNavigate();

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      const [coursesRes, assessmentsRes, questionsRes] = await Promise.all([
        getAdminCourses(),
        getAdminAssessments(),
        getAdminQuestions(),
      ]);

      setStats({
        courses: coursesRes.data.length,
        assessments: assessmentsRes.data.length,
        questions: questionsRes.data.length,
      });
    } catch (error) {
      console.error("Failed to load stats:", error);
    }
  };

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Admin workspace</div>
          <h1 className="page-title">Oversee courses, assessments, and question banks.</h1>
          <p className="page-subtitle">
            Manage the academic system from a brighter analytics-oriented control surface inspired by your exported Figma UI.
          </p>
        </div>

        <div className="hero-aside">
          <div className="hero-badge">
            <span className="hero-badge-label">Courses</span>
            <span className="hero-badge-value">{stats.courses}</span>
          </div>
          <div className="hero-badge">
            <span className="hero-badge-label">Assessments</span>
            <span className="hero-badge-value">{stats.assessments}</span>
          </div>
        </div>
      </section>

      <div className="stats-grid">
        <div className="stat-card interactive" onClick={() => navigate("/admin/courses")}>
          <div className="stat-label">Course Management</div>
          <div className="stat-value">{stats.courses}</div>
          <div className="stat-note">Create and update the current course catalog.</div>
        </div>
        <div className="stat-card interactive" onClick={() => navigate("/admin/assessments")}>
          <div className="stat-label">Assessment Center</div>
          <div className="stat-value">{stats.assessments}</div>
          <div className="stat-note">Maintain tests linked to courses and schedules.</div>
        </div>
        <div className="stat-card interactive" onClick={() => navigate("/admin/questions")}>
          <div className="stat-label">Question Bank</div>
          <div className="stat-value">{stats.questions}</div>
          <div className="stat-note">Review the pool of questions available to assessments.</div>
        </div>
      </div>

      <section className="surface-panel">
        <div className="section-heading">
          <div>
            <h2 className="section-title">Quick actions</h2>
            <p className="section-subtitle">Jump directly into the main administration flows.</p>
          </div>
        </div>

        <div className="card-grid mt-2">
          <article className="card">
            <h3>Courses</h3>
            <p>Build new course offerings and update instructors, descriptions, and duration.</p>
            <div className="card-actions">
              <button className="btn btn-primary btn-sm" onClick={() => navigate("/admin/courses")}>
                Open Courses
              </button>
            </div>
          </article>

          <article className="card">
            <h3>Assessments</h3>
            <p>Create tests aligned to courses and configure titles, descriptions, and duration.</p>
            <div className="card-actions">
              <button className="btn btn-primary btn-sm" onClick={() => navigate("/admin/assessments")}>
                Open Assessments
              </button>
            </div>
          </article>

          <article className="card">
            <h3>Questions</h3>
            <p>Extend the question bank and assign the right answer for each assessment item.</p>
            <div className="card-actions">
              <button className="btn btn-primary btn-sm" onClick={() => navigate("/admin/questions")}>
                Open Questions
              </button>
            </div>
          </article>
        </div>
      </section>
    </Layout>
  );
}
