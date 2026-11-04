import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Alert, ConfirmDialog, Field, Loading } from '../components/Common.jsx';
import { fullDate, socialErrorMessage } from '../format.js';

const PROVIDERS = [
  ['GOOGLE', 'Google', 'google'],
  ['KAKAO', '카카오', 'kakao'],
];

export default function SettingsPage() {
  const { user, refresh } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const [message, setMessage] = useState(null); // { type, text }

  useEffect(() => {
    const linked = searchParams.get('linked');
    const error = searchParams.get('error');
    if (linked) setMessage({ type: 'success', text: `${linked === 'kakao' ? '카카오' : 'Google'} 계정을 연결했습니다.` });
    if (error) setMessage({ type: 'error', text: socialErrorMessage(error) });
    if (linked || error) setSearchParams({}, { replace: true });
  }, [searchParams, setSearchParams]);

  if (!user) return <Loading />;

  return (
    <div className="stack narrow">
      <h1>설정</h1>
      {message && <Alert type={message.type}>{message.text}</Alert>}
      <ProfileSection user={user} refresh={refresh} />
      <IdentitySection user={user} refresh={refresh} setMessage={setMessage} />
      <NotificationSection user={user} refresh={refresh} />
    </div>
  );
}

function ProfileSection({ user, refresh }) {
  const [nickname, setNickname] = useState(user.nickname);
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    setSaved(false);
    try {
      await api('/api/me', { method: 'PATCH', body: { nickname } });
      await refresh();
      setSaved(true);
    } catch (err) {
      setError(err.fieldErrors?.nickname || err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="card">
      <h2>내 정보</h2>
      <dl className="details">
        <dt>이메일</dt>
        <dd>{user.email || <span className="muted">없음 (카카오에서 이메일 제공에 동의하지 않음)</span>}</dd>
      </dl>
      <form onSubmit={onSubmit} className="form inline-form">
        <Field label="닉네임" error={error}>
          <input type="text" maxLength={30} value={nickname} onChange={(e) => setNickname(e.target.value)} />
        </Field>
        <button type="submit" className="btn btn-primary" disabled={busy || nickname.trim() === user.nickname}>저장</button>
      </form>
      {saved && <p className="muted small">저장했습니다.</p>}
    </section>
  );
}

function IdentitySection({ user, refresh, setMessage }) {
  const [target, setTarget] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const connected = Object.fromEntries(user.identities.map((i) => [i.provider, i]));

  const onUnlink = async () => {
    setBusy(true);
    setError('');
    try {
      await api(`/api/me/identities/${target[2]}`, { method: 'DELETE' });
      await refresh();
      setMessage({ type: 'success', text: `${target[1]} 연결을 해제했습니다.` });
      setTarget(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="card">
      <h2>로그인 수단</h2>
      <ul className="list">
        <li className="list-row static">
          <span className="list-main">
            <strong>이메일 + 비밀번호</strong>
            <span className="muted small">
              {user.hasPassword ? '사용 중' : user.email ? '비밀번호 찾기로 비밀번호를 만들 수 있습니다' : '사용 안 함'}
            </span>
          </span>
        </li>
        {PROVIDERS.map((provider) => {
          const [key, label, path] = provider;
          const identity = connected[key];
          return (
            <li key={key} className="list-row static">
              <span className="list-main">
                <strong>{label}</strong>
                <span className="muted small">{identity ? `${fullDate(identity.connectedAt.slice(0, 10))} 연결` : '연결 안 됨'}</span>
              </span>
              {identity ? (
                <button type="button" className="btn btn-ghost btn-sm" onClick={() => setTarget(provider)}>연결 해제</button>
              ) : (
                <a className="btn btn-ghost btn-sm" href={`/oauth2/authorization/${path}`}>연결</a>
              )}
            </li>
          );
        })}
      </ul>
      {target && (
        <ConfirmDialog title="연결 해제" confirmText="해제" onConfirm={onUnlink} onCancel={() => { setTarget(null); setError(''); }} busy={busy}>
          <p>{target[1]} 계정으로는 더 이상 로그인할 수 없습니다.</p>
          <Alert>{error}</Alert>
        </ConfirmDialog>
      )}
    </section>
  );
}

function NotificationSection({ user, refresh }) {
  const [enabled, setEnabled] = useState(user.notification.enabled);
  const [daysBefore, setDaysBefore] = useState(user.notification.daysBefore);
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);

  if (!user.notification.available) {
    return (
      <section className="card">
        <h2>결제 알림</h2>
        <p className="muted">이메일이 없어 알림을 받을 수 없습니다. 이메일을 제공하는 로그인 수단을 연결해 주세요.</p>
      </section>
    );
  }

  const onSubmit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    setSaved(false);
    try {
      await api('/api/me/notification-settings', { method: 'PUT', body: { enabled, daysBefore: Number(daysBefore) } });
      await refresh();
      setSaved(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="card">
      <h2>결제 알림</h2>
      <p className="muted small">매일 오전 9시 전후에 곧 결제될 구독을 메일로 알려드립니다. 메일이 보이지 않으면 스팸함을 확인해 주세요.</p>
      <form onSubmit={onSubmit} className="form">
        <label className="checkbox">
          <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
          결제 알림 받기
        </label>
        <Field label="알림 시점">
          <select value={daysBefore} onChange={(e) => setDaysBefore(e.target.value)} disabled={!enabled}>
            <option value={1}>결제 1일 전</option>
            <option value={3}>결제 3일 전</option>
            <option value={7}>결제 7일 전</option>
          </select>
        </Field>
        <Alert>{error}</Alert>
        <div className="form-actions">
          <button type="submit" className="btn btn-primary" disabled={busy}>저장</button>
        </div>
        {saved && <p className="muted small">저장했습니다.</p>}
      </form>
    </section>
  );
}
