import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useOutletContext } from 'react-router';
import { api } from '../api.js';
import { Alert, EmptyState, Loading, PageHeader, ServiceLogo } from '../components/Common.jsx';
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

  return (
    <div className="page">
      <PageHeader
        title="알림"
        back="/"
        right={data?.unreadCount > 0
          ? <button type="button" className="btn btn-ghost btn-sm" onClick={readAll}>모두 읽음</button>
          : undefined}
      />
      <Alert>{error}</Alert>
      {!data && !error && <Loading />}
      {data && data.items.length === 0 && (
        <EmptyState action={<Link to="/settings" className="btn btn-soft">알림 설정</Link>}>
          아직 알림이 없어요.<br />결제일이 다가오면 여기에 알려드릴게요.
        </EmptyState>
      )}
      {data && data.items.length > 0 && (
        <section className="card card-tight">
          <ul className="rows">
            {data.items.map((item) => (
              <li key={item.id} className={item.read ? 'read' : 'unread'}>
                <button type="button" className="row" onClick={() => open(item)}>
                  <span className="noti-dot" aria-hidden="true" />
                  <ServiceLogo name={item.title.replace(/ 결제 예정$/, '')} size="sm" />
                  <span className="row-main">
                    <strong>{item.title}</strong>
                    <span className="muted small">{relativeDay(item.paymentDate)} · {item.body}</span>
                  </span>
                  <span className="muted small nowrap">{timeAgo(item.createdAt)}</span>
                </button>
                {!item.read && <span className="sr-only">안 읽음</span>}
              </li>
            ))}
          </ul>
        </section>
      )}
      <p className="note">알림은 90일 동안 보관돼요. 달러 결제의 원화 금액은 환율 기준 추정치예요.</p>
    </div>
  );
}
