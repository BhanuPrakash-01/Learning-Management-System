import { useEffect, useState } from "react";
import Layout from "../../components/Layout";
import {
  downloadAssessmentReport,
  downloadNonAttemptsReport,
  getAdminAssessments,
} from "../../services/adminService";

function downloadBlob(blob, fileName) {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  link.click();
  window.URL.revokeObjectURL(url);
}

export default function Reports() {
  const [assessments, setAssessments] = useState([]);
  const [selectedAssessment, setSelectedAssessment] = useState("");
  const [format, setFormat] = useState("csv");

  useEffect(() => {
    getAdminAssessments()
      .then((res) => setAssessments(res.data || []))
      .catch((err) => console.error(err));
  }, []);

  const downloadAssessment = async () => {
    if (!selectedAssessment) return;
    const res = await downloadAssessmentReport(selectedAssessment, format);
    downloadBlob(res.data, `assessment_${selectedAssessment}.${format}`);
  };

  const downloadNonAttempts = async () => {
    if (!selectedAssessment) return;
    const res = await downloadNonAttemptsReport(selectedAssessment, format);
    downloadBlob(res.data, `non_attempts_${selectedAssessment}.${format}`);
  };

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Reports</div>
          <h1 className="page-title">Download performance reports</h1>
          <p className="page-subtitle">Assessment report and non-attempt report exports in CSV or XLSX.</p>
        </div>
      </section>

      <section className="surface-panel">
        <div className="form-grid">
          <div className="form-group">
            <label htmlFor="assessmentId">Assessment</label>
            <select
              id="assessmentId"
              value={selectedAssessment}
              onChange={(e) => setSelectedAssessment(e.target.value)}
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
            <label htmlFor="format">Format</label>
            <select id="format" value={format} onChange={(e) => setFormat(e.target.value)}>
              <option value="csv">CSV</option>
              <option value="xlsx">XLSX</option>
            </select>
          </div>
        </div>
        <div className="card-actions mt-2">
          <button className="btn btn-primary btn-sm" onClick={downloadAssessment} disabled={!selectedAssessment}>
            Download Assessment Report
          </button>
          <button className="btn btn-secondary btn-sm" onClick={downloadNonAttempts} disabled={!selectedAssessment}>
            Download Non-Attempt Report
          </button>
        </div>
      </section>
    </Layout>
  );
}
