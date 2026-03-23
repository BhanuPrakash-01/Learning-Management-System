import { useEffect, useState } from "react";
import Layout from "../../components/Layout";
import { getLeaderboardConfig } from "../../services/adminService";

export default function LeaderboardConfig() {
  const [config, setConfig] = useState(null);

  useEffect(() => {
    getLeaderboardConfig()
      .then((res) => setConfig(res.data))
      .catch((err) => console.error(err));
  }, []);

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Leaderboard Config</div>
          <h1 className="page-title">Scoring weights and contribution rules</h1>
          <p className="page-subtitle">Current backend scoring configuration used for rankings.</p>
        </div>
      </section>

      <section className="surface-panel">
        {!config ? (
          <div className="empty-state">
            <p>Loading configuration...</p>
          </div>
        ) : (
          <div className="card-grid">
            {Object.entries(config).map(([key, value]) => (
              <article key={key} className="card">
                <h3>{key}</h3>
                <p>{String(value)}</p>
              </article>
            ))}
          </div>
        )}
      </section>
    </Layout>
  );
}
