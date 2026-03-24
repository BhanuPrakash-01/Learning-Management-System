import { useEffect, useMemo, useState } from "react";
import { getMe, logout as logoutApi } from "../services/authService";
import { AuthContext } from "./auth-context";

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getMe()
      .then((res) => setUser(res.data || null))
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  const login = (session) => {
    setUser(session || null);
  };

  const refreshUser = async () => {
    const res = await getMe();
    setUser(res.data || null);
    return res.data || null;
  };

  const logout = async () => {
    try {
      await logoutApi();
    } catch {
      // Ignore logout failures and clear local session state anyway.
    } finally {
      setUser(null);
    }
  };

  const value = useMemo(
    () => ({ user, loading, login, logout, refreshUser }),
    [user, loading]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
