import Layout from "../../components/Layout";

export default function AdminDashboard() {
  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Admin Dashboard</div>
          <h1 className="page-title">College-wide LMS analytics</h1>
          <p className="page-subtitle">
            Detailed analytics will be available in a future release.
          </p>
        </div>
      </section>
    </Layout>
  );
}
