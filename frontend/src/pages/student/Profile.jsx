import { useEffect, useState } from "react";
import Layout from "../../components/Layout";
import { getMyProfile, updateMyProfile } from "../../services/profileService";

export default function Profile() {
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({ name: "", phone: "" });
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    getMyProfile()
      .then((res) => {
        setProfile(res.data);
        setForm({
          name: res.data.name || "",
          phone: res.data.phone || "",
        });
      })
      .catch((err) => console.error(err));
  }, []);

  const saveProfile = async () => {
    setMessage("");
    setError("");
    try {
      const res = await updateMyProfile(form);
      setMessage(res.data.message || "Profile updated");
      const refreshed = await getMyProfile();
      setProfile(refreshed.data);
    } catch (err) {
      setError(err.response?.data?.error || "Failed to update profile.");
    }
  };

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">My Profile</div>
          <h1 className="page-title">Personal and academic profile</h1>
          <p className="page-subtitle">Academic fields are read-only and controlled by admin settings.</p>
        </div>
      </section>

      <section className="surface-panel">
        <div className="section-heading">
          <div>
            <h2 className="section-title">Academic Details (Read-only)</h2>
          </div>
        </div>
        <div className="card-grid mt-2">
          <article className="card">
            <h3>Roll Number</h3>
            <p>{profile?.rollNumber || "N/A"}</p>
          </article>
          <article className="card">
            <h3>Branch</h3>
            <p>{profile?.branch || "N/A"}</p>
          </article>
          <article className="card">
            <h3>Batch</h3>
            <p>{profile?.batchYear || "N/A"}</p>
          </article>
          <article className="card">
            <h3>Section</h3>
            <p>{profile?.section || "N/A"}</p>
          </article>
        </div>
      </section>

      <section className="surface-panel">
        <div className="section-heading">
          <div>
            <h2 className="section-title">Editable Details</h2>
          </div>
        </div>
        <div className="form-grid mt-2">
          <div className="form-group">
            <label htmlFor="name">Name</label>
            <input
              id="name"
              value={form.name}
              onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
            />
          </div>
          <div className="form-group">
            <label htmlFor="phone">Phone Number</label>
            <input
              id="phone"
              value={form.phone}
              onChange={(e) => setForm((prev) => ({ ...prev, phone: e.target.value }))}
              placeholder="10-digit number"
            />
          </div>
        </div>
        <div className="card-actions mt-2">
          <button className="btn btn-primary btn-sm" onClick={saveProfile}>
            Save Profile
          </button>
        </div>
        {message && <div className="alert alert-success mt-2">{message}</div>}
        {error && <div className="alert alert-error mt-2">{error}</div>}
      </section>

      <section className="surface-panel">
        <div className="section-heading">
          <div>
            <h2 className="section-title">Badges & Readiness</h2>
          </div>
        </div>
        <div className="card-grid mt-2">
          {(profile?.badges || []).map((badge) => (
            <article key={badge.id} className="card">
              <h3>{badge.badgeType}</h3>
              <p>{badge.earnedAt ? new Date(badge.earnedAt).toLocaleString() : "N/A"}</p>
            </article>
          ))}
          <article className="card">
            <h3>Placement Readiness Score</h3>
            <p>{profile?.placementReadinessScore ?? 0}</p>
          </article>
        </div>
      </section>
    </Layout>
  );
}
