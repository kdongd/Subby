import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useOutletContext } from 'react-router';
import { api } from '../api.js';
import { Alert, Loading } from '../components/Common.jsx';
import { relativeDay, timeAgo } from '../format.js';

export default function NotificationsPage() {
  const { refreshUnread } = useOutletContext();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      setData(await api('/api/notifications'));
      setError('');
    } catch (err) {
      setError(err.message);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const open = async (item) => {
    if (!item.read) {
      try {
        await api(`/api/notifications/${item.id}/read`, { method: 'POST' });
        refreshUnread();
      } catch {
        // 읽음 처리가 실패해도 화면 이동은 한다.
      }
    }
    navigate(item.link);
  };

  const readAll = async () => {
    try {
      await api('/api/notifications/read-all', { method: 'POST' });
      await load();
      refreshUnread();
    } catch (err) {
      setError(err.message);
    }
  };

  if (!data && !error) return <Loading />;

  return (
    <div className="stack narrow">
      <div className="page-head">
        <h1>알림</h1>
        {data?.unreadCount > 0 && (
          <button type="button" className="btn btn-ghost btn-sm" onClick={readAll}>모두 읽음</button>
        )}
      </div>
      <Alert>{error}</Alert>
      {data && data.items.length === 0 && (
        <div className="card empty">
          <p className="muted">아직 알림이 없어요. 결제일이 다가오면 여기에 알려드려요.</p>
          <Link to="/settings" className="btn btn-ghost btn-sm">알림 설정</Link>
        </div>
      )}
      {data && data.items.length > 0 && (
        <ul className="list card">
          {data.items.map((item) => (
            <li key={item.id}>
              <button type="button" className={`list-row notification-row${item.read ? '' : ' unread'}`} onClick={() => open(item)}>
                <span className="dot" aria-hidden="true" />
                <span className="list-main">
                  <strong>{item.title}</strong>
                  <span className="muted small">{relativeDay(item.paymentDate)} · {item.body}</span>
                </span>
                <span className="muted small nowrap">{timeAgo(item.createdAt)}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
      <p className="muted small">알림은 90일 동안 보관됩니다. 달러 결제의 원화 금액은 환율 기준 추정치입니다.</p>
    </div>
  );
}
