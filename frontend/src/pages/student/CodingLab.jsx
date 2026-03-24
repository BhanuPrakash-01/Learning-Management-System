import { useEffect, useState } from "react";
import Layout from "../../components/Layout";
import { getCodingProblems, submitCodingSolution } from "../../services/codingService";

const languages = ["Python", "Java", "C", "C++"];

export default function CodingLab() {
  const [problems, setProblems] = useState([]);
  const [selectedProblem, setSelectedProblem] = useState(null);
  const [language, setLanguage] = useState("Python");
  const [code, setCode] = useState("");
  const [verdict, setVerdict] = useState(null);
  const [difficultyFilter, setDifficultyFilter] = useState("");

  useEffect(() => {
    getCodingProblems(difficultyFilter ? { difficulty: difficultyFilter } : {})
      .then((res) => setProblems(res.data || []))
      .catch((err) => console.error(err));
  }, [difficultyFilter]);

  const runCode = async () => {
    if (!selectedProblem) return;
    const res = await submitCodingSolution({
      problemId: selectedProblem.id,
      language,
      code,
    });
    setVerdict(res.data);
  };

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Coding Lab</div>
          <h1 className="page-title">Practice coding with run and submit workflow</h1>
          <p className="page-subtitle">
            Choose a problem by difficulty/topic and submit code in Python, Java, C, or C++.
          </p>
        </div>
      </section>

      <section className="surface-panel">
        <div className="card-actions">
          <button className={`btn ${difficultyFilter === "" ? "btn-primary" : "btn-secondary"} btn-sm`} onClick={() => setDifficultyFilter("")}>
            All
          </button>
          {["EASY", "MEDIUM", "HARD"].map((level) => (
            <button
              key={level}
              className={`btn ${difficultyFilter === level ? "btn-primary" : "btn-secondary"} btn-sm`}
              onClick={() => setDifficultyFilter(level)}
            >
              {level}
            </button>
          ))}
        </div>
      </section>

      <div className="card-grid">
        <section className="card">
          <h3>Problem List</h3>
          <div className="card-actions" style={{ flexDirection: "column", alignItems: "stretch" }}>
            {problems.map((problem) => (
              <button key={problem.id} className="btn btn-secondary btn-sm" onClick={() => setSelectedProblem(problem)}>
                {problem.title} ({problem.difficulty})
              </button>
            ))}
          </div>
        </section>

        <section className="card">
          {!selectedProblem ? (
            <div className="empty-state">
              <p>Select a problem to start coding.</p>
            </div>
          ) : (
            <>
              <h3>{selectedProblem.title}</h3>
              <p>{selectedProblem.description}</p>
              <p style={{ color: "var(--text-muted)" }}>Topic: {selectedProblem.topic}</p>

              <div className="form-group">
                <label htmlFor="language">Language</label>
                <select id="language" value={language} onChange={(e) => setLanguage(e.target.value)}>
                  {languages.map((lang) => (
                    <option key={lang} value={lang}>
                      {lang}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="editor">Code</label>
                <textarea
                  id="editor"
                  rows={12}
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  placeholder="Write your code here..."
                />
              </div>

              <div className="card-actions">
                <button className="btn btn-secondary btn-sm" onClick={runCode}>
                  Run
                </button>
                <button className="btn btn-primary btn-sm" onClick={runCode}>
                  Submit
                </button>
              </div>

              {verdict && (
                <div className={`alert ${verdict.status === "ACCEPTED" ? "alert-success" : "alert-error"}`} style={{ marginTop: "1rem" }}>
                  <p>Status: {verdict.status}</p>
                  <p>
                    Passed test cases: {verdict.testCasesPassed}
                    {typeof verdict.totalTestCases === "number" ? ` / ${verdict.totalTestCases}` : ""}
                  </p>
                  {(verdict.details || []).length > 0 && (
                    <div style={{ marginTop: "0.5rem" }}>
                      {(verdict.details || []).slice(0, 3).map((detail, index) => (
                        <div key={index} style={{ marginBottom: "0.4rem", fontSize: "0.85rem" }}>
                          <strong>Case {detail.testCase || index + 1}:</strong>{" "}
                          expected <code>{detail.expectedOutput || "-"}</code>, got{" "}
                          <code>{detail.actualOutput || detail.stderr || detail.compileOutput || "-"}</code>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </>
          )}
        </section>
      </div>
    </Layout>
  );
}
