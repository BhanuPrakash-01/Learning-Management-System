import { useContext } from "react";
import Layout from "../../components/Layout";
import { AuthContext } from "../../context/auth-context";

export default function AdminProfile() {
  const { user } = useContext(AuthContext);

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Admin Profile</div>
          <h1 className="page-title">{user?.name || "Administrator"}</h1>
          <p className="page-subtitle">Manage your admin account details and security settings.</p>
        </div>
      </section>

      <section className="surface-panel">
        <div className="card-grid">
          <article className="card">
            <h3>Email</h3>
            <p>{user?.email || "N/A"}</p>
          </article>
          <article className="card">
            <h3>Role</h3>
            <p>{user?.role || "ADMIN"}</p>
          </article>
        </div>
      </section>
    </Layout>
  );
}
