import { useCallback, useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';

export default function Layout() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [unread, setUnread] = useState(0);

  const refreshUnread = useCallback(async () => {
    try {
      const { unreadCount } = await api('/api/notifications/unread-count');
      setUnread(unreadCount);
    } catch {
      // 숫자 표시는 실패해도 화면 사용에 지장이 없다.
    }
  }, []);

  // 화면을 옮길 때, 창으로 돌아올 때, 푸시를 받았을 때 숫자를 갱신한다.
  useEffect(() => {
    refreshUnread();
  }, [location.pathname, refreshUnread]);

  useEffect(() => {
    const onVisible = () => document.visibilityState === 'visible' && refreshUnread();
    const onMessage = (event) => event.data?.type === 'subby:push' && refreshUnread();
    document.addEventListener('visibilitychange', onVisible);
    navigator.serviceWorker?.addEventListener('message', onMessage);
    return () => {
      document.removeEventListener('visibilitychange', onVisible);
      navigator.serviceWorker?.removeEventListener('message', onMessage);
    };
  }, [refreshUnread]);

  return (
    <div className="app">
      <header className="topbar">
        <div className="topbar-inner">
          <NavLink to="/" className="brand">서비</NavLink>
          <nav className="nav">
            <NavLink to="/" end>대시보드</NavLink>
            <NavLink to="/subscriptions">구독</NavLink>
            <NavLink to="/stats">통계</NavLink>
            <NavLink to="/settings">설정</NavLink>
          </nav>
          <div className="topbar-user">
            <NavLink to="/notifications" className="bell" aria-label={unread > 0 ? `알림 ${unread}개 안 읽음` : '알림'}>
              <span aria-hidden="true">🔔</span>
              {unread > 0 && <span className="bell-badge">{unread > 99 ? '99+' : unread}</span>}
            </NavLink>
            <span className="muted hide-sm">{user?.nickname}님</span>
            <button type="button" className="btn btn-ghost btn-sm" onClick={logout}>로그아웃</button>
          </div>
        </div>
      </header>
      <main className="container">
        <Outlet context={{ refreshUnread }} />
      </main>
    </div>
  );
}
