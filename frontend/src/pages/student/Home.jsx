import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Layout from "../../components/Layout";
import { getStudentHome } from "../../services/homeService";
import { getStudentNotifications, markNotificationRead } from "../../services/notificationService";

function formatDateTime(value) {
  if (!value) return "N/A";
  return new Date(value).toLocaleString();
}

export default function StudentHome() {
  const [data, setData] = useState(null);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notificationsLoading, setNotificationsLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    getStudentHome()
      .then((res) => setData(res.data))
      .catch((err) => console.error(err));

    getStudentNotifications({ page: 0, size: 6 })
      .then((res) => {
        setNotifications(res.data?.content || []);
        setUnreadCount(res.data?.unreadCount || 0);
      })
      .catch((err) => console.error(err))
      .finally(() => setNotificationsLoading(false));
  }, []);

  const handleMarkRead = async (id) => {
    try {
      await markNotificationRead(id);
      setNotifications((prev) =>
        prev.map((item) => (item.id === id ? { ...item, read: true } : item))
      );
      setUnreadCount((prev) => Math.max(prev - 1, 0));
    } catch (err) {
      console.error(err);
    }
  };

  if (!data) {
    return (
      <Layout>
        <section className="surface-panel">
          <div className="loading-skeleton" style={{ minHeight: "180px" }} />
        </section>
        <section className="surface-panel">
          <div className="loading-skeleton" style={{ minHeight: "280px" }} />
        </section>
      </Layout>
    );
  }

  return (
    <Layout>
      <section className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">Student Home</div>
          <h1 className="page-title">Welcome back{data?.student?.name ? `, ${data.student.name}` : ""}</h1>
          <p className="page-subtitle">
            {`${data.student.rollNumber} · ${data.student.branch} · ${data.student.batchYear} · Section ${data.student.section}`}
          </p>
        </div>

        <div className="hero-aside">
          <div className="hero-badge">
            <span className="hero-badge-label">Leaderboard Score</span>
            <span className="hero-badge-value">{data?.leaderboardScore ?? 0}</span>
          </div>
          <div className="hero-badge">
            <span className="hero-badge-label">Current Streak</span>
            <span className="hero-badge-value">{data?.currentStreak ?? 0} days</span>
          </div>
        </div>
      </section>

      <section className="surface-panel">
        <div className="section-heading">
          <div>
            <h2 className="section-title">Upcoming Assessments</h2>
            <p className="section-subtitle">Next targeted assessments for your branch and batch.</p>
          </div>
          <button className="btn btn-secondary btn-sm" onClick={() => navigate("/student/assessments")}>
            View All
          </button>
        </div>

        {!data?.upcoming?.length ? (
          <div className="empty-state mt-2">
            <p>No upcoming assessments at the moment.</p>
          </div>
        ) : (
          <div className="card-grid mt-2">
            {data.upcoming.map((item) => (
              <article key={item.id} className="card">
                <h3>{item.title}</h3>
                <p>{item.description || "No description available."}</p>
                <div className="card-meta">
                  <div className="meta-item">
                    <span>Type</span>
                    <strong>{item.assessmentType || "GENERAL"}</strong>
                  </div>
                  <div className="meta-item">
                    <span>Starts</span>
                    <strong>{formatDateTime(item.startTime)}</strong>
                  </div>
                  <div className="meta-item">
                    <span>Duration</span>
                    <strong>{item.duration} min</strong>
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      <section className="surface-panel">
        <div className="section-heading">
          <div>
            <h2 className="section-title">Notifications</h2>
            <p className="section-subtitle">Upcoming assessment reminders and alerts.</p>
          </div>
          <span className={`badge ${unreadCount > 0 ? "badge-warning" : "badge-neutral"}`}>
            {unreadCount} unread
          </span>
        </div>

        {notificationsLoading ? (
          <div className="loading-skeleton mt-2" style={{ minHeight: "140px" }} />
        ) : !notifications.length ? (
          <div className="empty-state mt-2">
            <p>No notifications yet.</p>
          </div>
        ) : (
          <div className="notification-list mt-2">
            {notifications.map((item) => (
              <article key={item.id} className={`notification-item${item.read ? "" : " unread"}`}>
                <div>
                  <h3>{item.title}</h3>
                  <p>{item.message}</p>
                  <small>{formatDateTime(item.createdAt)}</small>
                </div>
                {!item.read && (
                  <button
                    className="btn btn-secondary btn-sm"
                    type="button"
                    onClick={() => handleMarkRead(item.id)}
                  >
                    Mark Read
                  </button>
                )}
              </article>
            ))}
          </div>
        )}
      </section>

    </Layout>
  );
}
