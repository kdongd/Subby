import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router';
import { api, setUnauthorizedHandler } from './api.js';
import { detachPushOnLogout } from './push.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  // 내 정보를 다시 불러온다. 로그인 안 했으면 null. 이 요청이 CSRF 쿠키도 받아온다.
  const refresh = useCallback(async () => {
    try {
      const me = await api('/api/me', { skipAuthRedirect: true });
      setUser(me);
      return me;
    } catch {
      setUser(null);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  useEffect(() => {
    setUnauthorizedHandler(() => {
      setUser(null);
      navigate('/login', { replace: true });
    });
  }, [navigate]);

  const login = useCallback(async (email, password) => {
    await api('/api/auth/login', { method: 'POST', body: { email, password }, skipAuthRedirect: true });
    return refresh();
  }, [refresh]);

  const signup = useCallback(async (form) => {
    await api('/api/auth/signup', { method: 'POST', body: form });
    return refresh();
  }, [refresh]);

  const logout = useCallback(async () => {
    await detachPushOnLogout();
    try {
      await api('/api/auth/logout', { method: 'POST' });
    } finally {
      setUser(null);
      navigate('/login', { replace: true });
    }
  }, [navigate]);

  const value = useMemo(
    () => ({ user, loading, refresh, login, signup, logout }),
    [user, loading, refresh, login, signup, logout],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
