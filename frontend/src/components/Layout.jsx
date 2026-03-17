import Navbar from "./Navbar";

export default function Layout({ children }) {
  return (
    <div className="app-shell">
      <Navbar />
      <main className="page-container">
        <div className="page-stack">{children}</div>
      </main>
    </div>
  );
}
