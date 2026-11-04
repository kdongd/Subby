import { useCallback, useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router';
import { api } from '../api.js';
import { ChartIcon, HomeIcon, ListIcon, UserIcon } from './Icons.jsx';

const TABS = [
  ['/', '홈', HomeIcon, true],
  ['/subscriptions', '구독', ListIcon, false],
  ['/stats', '통계', ChartIcon, false],
  ['/settings', '내 정보', UserIcon, false],
];

/** 로그인 후 화면 틀: 내용 + 하단 탭바. 안 읽은 알림 수는 여기서 관리해 화면들에 나눠 준다. */
export default function Layout() {
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

  // 화면을 옮길 때, 앱으로 돌아올 때, 푸시를 받았을 때 숫자를 갱신한다.
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

  // 화면을 옮기면 맨 위부터 보이게 한다.
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location.pathname]);

  return (
    <div className="app">
      <main>
        <Outlet context={{ unread, refreshUnread }} />
      </main>
      <nav className="tabbar" aria-label="주요 메뉴">
        {TABS.map(([to, label, Icon, end]) => (
          <NavLink key={to} to={to} end={end}>
            <Icon size={24} />
            {label}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
