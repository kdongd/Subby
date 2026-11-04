import { useCallback, useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Alert, ConfirmDialog, Field, Loading, PageHeader } from '../components/Common.jsx';
import { BellIcon, ChevronIcon, LogoutIcon, ShieldIcon } from '../components/Icons.jsx';
import { fullDate, socialErrorMessage } from '../format.js';
import { logoUrl } from '../services.js';
import { disablePush, enablePush, permission, pushStatus, pushSupport } from '../push.js';

const PROVIDERS = [
  ['GOOGLE', 'Google', 'google'],
  ['KAKAO', '카카오', 'kakao'],
];

export default function SettingsPage() {
  const { user, refresh, logout } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const [message, setMessage] = useState(null); // { type, text }

  useEffect(() => {
    const linked = searchParams.get('linked');
    const error = searchParams.get('error');
    if (linked) setMessage({ type: 'success', text: `${linked === 'kakao' ? '카카오' : 'Google'} 계정을 연결했어요.` });
    if (error) setMessage({ type: 'error', text: socialErrorMessage(error) });
    if (linked || error) setSearchParams({}, { replace: true });
  }, [searchParams, setSearchParams]);

  if (!user) return <Loading />;

  return (
    <div className="page">
      <PageHeader title="내 정보" />
      <Alert type={message?.type}>{message?.text}</Alert>
      <ProfileSection user={user} refresh={refresh} />
      <NotificationSection user={user} refresh={refresh} />
      <IdentitySection user={user} refresh={refresh} setMessage={setMessage} />

      <section className="card card-tight">
        <ul className="rows link-rows">
          <li>
            <Link to="/notifications" className="row">
              <BellIcon size={22} />
              <span className="row-main"><strong>알림 목록</strong></span>
              <ChevronIcon className="chev" />
            </Link>
          </li>
          <li>
            <Link to="/privacy" className="row">
              <ShieldIcon size={22} />
              <span className="row-main"><strong>개인정보처리방침</strong></span>
              <ChevronIcon className="chev" />
            </Link>
          </li>
          <li>
            <button type="button" className="row" onClick={logout}>
              <LogoutIcon size={22} />
              <span className="row-main"><strong>로그아웃</strong></span>
            </button>
          </li>
        </ul>
      </section>
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
    <section className="card section">
      <div className="profile">
        <img src="/images/face.webp" alt="" width="394" height="321" />
        <div>
          <p className="profile-name">{user.nickname}님</p>
          <p className="muted small">{user.email || '이메일 없음 (카카오에서 이메일 제공에 동의하지 않음)'}</p>
        </div>
      </div>
      <form onSubmit={onSubmit} className="form">
        <Field label="닉네임" error={error}>
          <input type="text" maxLength={30} value={nickname} onChange={(e) => { setNickname(e.target.value); setSaved(false); }} />
        </Field>
        <button type="submit" className="btn btn-soft" disabled={busy || !nickname.trim() || nickname.trim() === user.nickname}>
          {saved ? '저장했어요' : '닉네임 저장'}
        </button>
      </form>
    </section>
  );
}

function IdentitySection({ user, refresh, setMessage }) {
  const [target, setTarget] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const connected = Object.fromEntries(user.identities.map((i) => [i.provider, i]));
  // 화면에서는 소셜 로그인만 쓰므로, 마지막 소셜 연결을 해제하면 로그인할 방법이 없어진다.
  const canUnlink = user.identities.length > 1;

  const onUnlink = async () => {
    setBusy(true);
    setError('');
    try {
      await api(`/api/me/identities/${target[2]}`, { method: 'DELETE' });
      await refresh();
      setMessage({ type: 'success', text: `${target[1]} 연결을 해제했어요.` });
      setTarget(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="card section">
      <h2>로그인 수단</h2>
      <ul className="rows">
        {PROVIDERS.map((provider) => {
          const [key, label, path] = provider;
          const identity = connected[key];
          return (
            <li key={key}>
              <div className="row" style={{ padding: '10px 0', cursor: 'default' }}>
                {key === 'GOOGLE' ? (
                  <span className="logo logo-sm">
                    <img src={logoUrl('google.com')} alt="" onError={(e) => { e.currentTarget.style.visibility = 'hidden'; }} />
                  </span>
                ) : (
                  <span className="logo logo-sm" style={{ background: '#fee500', border: 0 }}>
                    <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true"><path fill="#191919" d="M12 3C6.5 3 2 6.5 2 10.8c0 2.8 1.9 5.2 4.7 6.6l-1 3.7c-.1.3.3.6.6.4l4.4-2.9c.4 0 .9.1 1.3.1 5.5 0 10-3.5 10-7.9S17.5 3 12 3z" /></svg>
                  </span>
                )}
                <span className="row-main">
                  <strong>{label}</strong>
                  <span className="muted small">{identity ? `${fullDate(identity.connectedAt.slice(0, 10))} 연결` : '연결 안 됨'}</span>
                </span>
                {identity
                  ? canUnlink && <button type="button" className="btn btn-ghost btn-sm" onClick={() => setTarget(provider)}>해제</button>
                  : <a className="btn btn-soft btn-sm" href={`/oauth2/authorization/${path}`}>연결</a>}
              </div>
            </li>
          );
        })}
      </ul>
      {!canUnlink && <p className="field-hint">연결된 계정이 하나뿐이면 해제할 수 없어요. 다른 계정을 먼저 연결해 주세요.</p>}
      {target && (
        <ConfirmDialog title="연결 해제" confirmText="해제하기" onConfirm={onUnlink} onCancel={() => { setTarget(null); setError(''); }} busy={busy}>
          <p>{target[1]} 계정으로는 더 이상 로그인할 수 없어요.</p>
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
  const changed = enabled !== user.notification.enabled || Number(daysBefore) !== user.notification.daysBefore;

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
    <section className="card section">
      <h2>결제 알림</h2>
      <p className="muted small">매일 오전 9시쯤 곧 결제될 구독을 🔔 알림으로 알려드려요. 아래에서 이 기기 푸시를 켜면 휴대폰 알림으로도 받아요.</p>
      <form onSubmit={onSubmit} className="form">
        <label className="check">
          <input type="checkbox" checked={enabled} onChange={(e) => { setEnabled(e.target.checked); setSaved(false); }} />
          결제 알림 받기
        </label>
        <div className="field">
          <span className="field-label">알림 시점</span>
          <div className="segmented light" role="radiogroup" aria-label="알림 시점">
            {[1, 3, 7].map((d) => (
              <button key={d} type="button" role="radio" aria-checked={Number(daysBefore) === d} disabled={!enabled}
                      className={Number(daysBefore) === d ? 'active' : ''} onClick={() => { setDaysBefore(d); setSaved(false); }}>
                {d}일 전
              </button>
            ))}
          </div>
        </div>
        <Alert>{error}</Alert>
        <button type="submit" className="btn btn-soft" disabled={busy || !changed}>{saved && !changed ? '저장했어요' : '알림 설정 저장'}</button>
      </form>
      <DevicePush notificationsEnabled={user.notification.enabled} />
    </section>
  );
}

function DevicePush({ notificationsEnabled }) {
  const support = pushSupport();
  const [state, setState] = useState(null); // { subscribed, deviceCount, serverEnabled }
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null); // { type, text }

  const load = useCallback(async () => {
    try {
      const [{ enabled: serverEnabled }, status] = await Promise.all([
        api('/api/push/public-key'),
        support === 'supported' ? pushStatus() : api('/api/push/status', { method: 'POST', body: {} }),
      ]);
      setState({ ...status, serverEnabled });
    } catch (err) {
      setMessage({ type: 'error', text: err.message });
    }
  }, [support]);

  useEffect(() => {
    load();
  }, [load]);

  const run = async (action, successText) => {
    setBusy(true);
    setMessage(null);
    try {
      const result = await action();
      await load();
      setMessage({ type: 'success', text: typeof successText === 'function' ? successText(result) : successText });
    } catch (err) {
      setMessage({ type: 'error', text: err.message });
    } finally {
      setBusy(false);
    }
  };

  const onEnable = () => run(enablePush, '이 기기에서 푸시를 받도록 설정했어요.');
  const onDisable = () => run(disablePush, '이 기기의 푸시를 껐어요.');
  const onTest = () => run(
    () => api('/api/push/test', { method: 'POST' }),
    (r) => (r.sent > 0 ? `테스트 알림을 보냈어요 (${r.sent}/${r.devices}대). 몇 초 안에 도착해요.` : '보내지 못했어요. 잠시 후 다시 시도해 주세요.'),
  );

  return (
    <div className="section" style={{ borderTop: '1px solid var(--line)', paddingTop: 16 }}>
      <h3>이 기기 푸시</h3>
      {support === 'ios-install' && (
        <div className="push-guide">
          <p>iPhone·iPad는 <b>홈 화면에 추가한 서비 앱</b>에서만 푸시를 받을 수 있어요 (iOS 16.4 이상).</p>
          <ol>
            <li>Safari 공유 버튼(□↑) → <b>홈 화면에 추가</b> (iOS 26은 ⋯ → 공유)</li>
            <li><b>웹 앱으로 열기</b>를 켜고 추가</li>
            <li>홈 화면의 서비로 열고 로그인한 뒤 여기서 푸시 켜기</li>
          </ol>
        </div>
      )}
      {support === 'unsupported' && <p className="muted small">이 브라우저는 웹 푸시를 지원하지 않아요. Chrome, Edge, Safari, Firefox 최신 버전을 써 주세요.</p>}
      {support === 'insecure' && <p className="muted small">https 주소에서만 푸시를 켤 수 있어요.</p>}

      {state && !state.serverEnabled && <p className="muted small">서버에 푸시가 아직 설정되지 않았어요. 🔔 알림은 그대로 받을 수 있어요.</p>}

      {state && state.serverEnabled && support === 'supported' && (
        <>
          <p className="small">
            {state.subscribed ? '이 기기에서 푸시를 받고 있어요.' : '이 기기에서는 푸시를 받지 않고 있어요.'}
            <span className="muted"> · 푸시 받는 기기 {state.deviceCount}대</span>
          </p>
          {permission() === 'denied' && (
            <p className="muted small">알림 권한이 차단돼 있어요. 브라우저 주소창 왼쪽 자물쇠(또는 휴대폰 설정)에서 알림을 허용한 뒤 다시 시도해 주세요.</p>
          )}
          {!notificationsEnabled && <p className="muted small">위의 “결제 알림 받기”가 꺼져 있으면 푸시도 오지 않아요.</p>}
          {state.subscribed ? (
            <div className="btn-row">
              <button type="button" className="btn btn-ghost" onClick={onTest} disabled={busy}>테스트 알림</button>
              <button type="button" className="btn btn-ghost" onClick={onDisable} disabled={busy}>푸시 끄기</button>
            </div>
          ) : (
            <button type="button" className="btn btn-primary btn-block" onClick={onEnable} disabled={busy}>이 기기에서 푸시 받기</button>
          )}
        </>
      )}
      {message && <Alert type={message.type}>{message.text}</Alert>}
    </div>
  );
}
