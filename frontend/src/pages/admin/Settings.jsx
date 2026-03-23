import { useEffect, useState } from "react";
import Layout from "../../components/Layout";
import { getAdminSettings } from "../../services/adminService";

export default function Settings() {
  const [settings, setSettings] = useState(null);

  useEffect(() => {
    getAdminSettings()
      .then((res) => setSettings(res.data))
      .catch((err) => console.error(err));
  }, []);

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Settings</div>
          <h1 className="page-title">Academic targeting settings</h1>
          <p className="page-subtitle">Configured branch list, batch years, sections, and accepted email domain.</p>
        </div>
      </section>

      <section className="surface-panel">
        {!settings ? (
          <div className="empty-state">
            <p>Loading settings...</p>
          </div>
        ) : (
          <div className="card-grid">
            <article className="card">
              <h3>Email Domain</h3>
              <p>{settings.emailDomain}</p>
            </article>
            <article className="card">
              <h3>Branches</h3>
              <p>{(settings.branches || []).join(", ")}</p>
            </article>
            <article className="card">
              <h3>Batch Years</h3>
              <p>{(settings.batchYears || []).join(", ")}</p>
            </article>
            <article className="card">
              <h3>Sections</h3>
              <p>{(settings.sections || []).join(", ")}</p>
            </article>
          </div>
        )}
      </section>
    </Layout>
  );
}
