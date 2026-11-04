import { NavLink, Outlet } from 'react-router';
import { useAuth } from '../auth.jsx';

export default function Layout() {
  const { user, logout } = useAuth();
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
            <span className="muted hide-sm">{user?.nickname}님</span>
            <button type="button" className="btn btn-ghost btn-sm" onClick={logout}>로그아웃</button>
          </div>
        </div>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </div>
  );
}
