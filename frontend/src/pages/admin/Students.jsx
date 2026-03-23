import { useEffect, useState } from "react";
import Layout from "../../components/Layout";
import { deactivateStudent, getStudentDetail, getStudents, resetStudentPassword } from "../../services/adminService";

export default function Students() {
  const [filters, setFilters] = useState({
    search: "",
    branch: "",
    batchYear: "",
    section: "",
  });
  const [rows, setRows] = useState([]);
  const [selected, setSelected] = useState(null);

  const load = async () => {
    const res = await getStudents({
      ...filters,
      batchYear: filters.batchYear || undefined,
    });
    setRows(res.data.content || []);
  };

  useEffect(() => {
    load().catch((err) => console.error(err));
  }, []);

  const applyFilters = () => {
    load().catch((err) => console.error(err));
  };

  const openProfile = async (id) => {
    const res = await getStudentDetail(id);
    setSelected(res.data);
  };

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Students</div>
          <h1 className="page-title">Student manager</h1>
          <p className="page-subtitle">
            Search and filter by branch, batch year, and section. Manage profiles and account actions.
          </p>
        </div>
      </section>

      <section className="surface-panel">
        <div className="form-grid">
          <div className="form-group">
            <label htmlFor="search">Search</label>
            <input
              id="search"
              value={filters.search}
              onChange={(e) => setFilters((prev) => ({ ...prev, search: e.target.value }))}
              placeholder="Name or roll number"
            />
          </div>
          <div className="form-group">
            <label htmlFor="branch">Branch</label>
            <input
              id="branch"
              value={filters.branch}
              onChange={(e) => setFilters((prev) => ({ ...prev, branch: e.target.value }))}
              placeholder="CSE"
            />
          </div>
          <div className="form-group">
            <label htmlFor="batch">Batch Year</label>
            <input
              id="batch"
              type="number"
              value={filters.batchYear}
              onChange={(e) => setFilters((prev) => ({ ...prev, batchYear: e.target.value }))}
              placeholder="2025"
            />
          </div>
          <div className="form-group">
            <label htmlFor="section">Section</label>
            <input
              id="section"
              value={filters.section}
              onChange={(e) => setFilters((prev) => ({ ...prev, section: e.target.value }))}
              placeholder="A"
            />
          </div>
        </div>
        <div className="card-actions mt-2">
          <button className="btn btn-primary btn-sm" onClick={applyFilters}>
            Apply Filters
          </button>
        </div>
      </section>

      <section className="surface-panel">
        {!rows.length ? (
          <div className="empty-state">
            <p>No students found.</p>
          </div>
        ) : (
          <div style={{ overflowX: "auto" }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Roll No</th>
                  <th>Name</th>
                  <th>Branch</th>
                  <th>Batch</th>
                  <th>Section</th>
                  <th>Assessments</th>
                  <th>Avg Score</th>
                  <th>Last Active</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.id}>
                    <td>{row.rollNumber}</td>
                    <td>{row.name}</td>
                    <td>{row.branch}</td>
                    <td>{row.batchYear}</td>
                    <td>{row.section}</td>
                    <td>{row.assessmentsTaken}</td>
                    <td>{Math.round((row.avgScore || 0) * 100) / 100}</td>
                    <td>{row.lastActive ? new Date(row.lastActive).toLocaleString() : "N/A"}</td>
                    <td>
                      <div className="card-actions">
                        <button className="btn btn-secondary btn-sm" onClick={() => openProfile(row.id)}>
                          View
                        </button>
                        <button className="btn btn-secondary btn-sm" onClick={() => resetStudentPassword(row.id)}>
                          Reset Password
                        </button>
                        <button className="btn btn-danger btn-sm" onClick={() => deactivateStudent(row.id)}>
                          Deactivate
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {selected && (
        <section className="surface-panel">
          <div className="section-heading">
            <div>
              <h2 className="section-title">{selected.student?.name}</h2>
              <p className="section-subtitle">{selected.student?.rollNumber}</p>
            </div>
            <button className="btn btn-danger btn-sm" onClick={() => setSelected(null)}>
              Close
            </button>
          </div>
          <div className="card-grid mt-2">
            {(selected.attempts || []).map((attempt) => (
              <article key={attempt.id} className="card">
                <h3>{attempt.assessment?.title || "Assessment"}</h3>
                <p>Score: {attempt.score}</p>
                <p>{attempt.endTime ? new Date(attempt.endTime).toLocaleString() : "In progress"}</p>
              </article>
            ))}
          </div>
        </section>
      )}
    </Layout>
  );
}
