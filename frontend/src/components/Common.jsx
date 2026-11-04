import { useEffect, useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router';
import { useAuth } from '../auth.jsx';
import { findService, initialOf, initialStyle, logoUrl } from '../services.js';
import { BackIcon } from './Icons.jsx';

/** 화면 안 로딩 표시. 무료 서버가 잠들어 있으면 첫 응답에 최대 1분이 걸리므로 3초가 지나면 안내를 덧붙인다. */
export function Loading({ text = '불러오는 중...' }) {
  const slow = useSlow();
  return (
    <div className="loading" role="status">
      <div className="spinner" aria-hidden="true" />
      <p>{text}</p>
      {slow && <p className="small">서버를 깨우는 중이에요. 첫 접속은 최대 1분 정도 걸릴 수 있어요.</p>}
    </div>
  );
}

function useSlow(ms = 3000) {
  const [slow, setSlow] = useState(false);
  useEffect(() => {
    const timer = setTimeout(() => setSlow(true), ms);
    return () => clearTimeout(timer);
  }, [ms]);
  return slow;
}

/** 스플래시 화면 위쪽(제목 + 그림). 로그인 화면과 첫 로딩 화면이 같이 쓴다. */
export function SplashArt() {
  return (
    <>
      <div className="splash-snow" aria-hidden="true" />
      <div className="splash-top">
        <h1 className="splash-title">서비<small>Subby</small></h1>
        <p className="splash-sub">나의 구독, 놓치지 않게 챙겨주는 펭귄 비서</p>
      </div>
      <div className="splash-art">
        <img src="/images/splash.webp" alt="" width="978" height="882" />
      </div>
    </>
  );
}

/** 앱을 처음 열 때 로그인 상태를 확인하는 동안 보여주는 화면 */
export function SplashScreen() {
  const slow = useSlow();
  return (
    <div className="app">
      <div className="splash">
        <SplashArt />
        <div className="splash-loading" role="status">
          <div className="spinner" aria-hidden="true" />
          <p>{slow ? '서버를 깨우는 중이에요. 최대 1분 정도 걸릴 수 있어요.' : '서비가 준비하고 있어요'}</p>
        </div>
      </div>
    </div>
  );
}

export function RequireAuth({ children }) {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) return <SplashScreen />;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return children;
}

export function GuestOnly({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <SplashScreen />;
  if (user) return <Navigate to="/" replace />;
  return children;
}

export function Alert({ type = 'error', children }) {
  if (!children) return null;
  return <div className={`alert alert-${type}`} role={type === 'error' ? 'alert' : 'status'}>{children}</div>;
}

export function Field({ label, error, hint, children }) {
  return (
    <label className="field">
      <span className="field-label">{label}</span>
      {children}
      {hint && !error && <span className="field-hint">{hint}</span>}
      {error && <span className="field-error">{error}</span>}
    </label>
  );
}

/** 화면 위쪽 제목줄. back을 주면 왼쪽에 뒤로 가기 버튼이 생긴다. */
export function PageHeader({ title, back, right }) {
  const navigate = useNavigate();
  const goBack = () => {
    // 알림에서 바로 들어온 경우처럼 앱 안의 이전 화면이 없으면 지정한 화면으로 간다.
    if (window.history.state?.idx > 0) navigate(-1);
    else navigate(back, { replace: true });
  };
  return (
    <header className="page-header">
      {back && (
        <button type="button" className="icon-btn plain" onClick={goBack} aria-label="뒤로">
          <BackIcon />
        </button>
      )}
      <h1 className={back ? 'center' : ''}>{title}</h1>
      {right || (back && <span style={{ width: 44 }} aria-hidden="true" />)}
    </header>
  );
}

/** 아래에서 올라오는 확인 창. 삭제·해지처럼 되돌리기 어려운 동작 전에 띄운다. */
export function ConfirmDialog({ title, children, confirmText, danger, onConfirm, onCancel, busy }) {
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && !busy && onCancel();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [busy, onCancel]);

  return (
    <div className="sheet-backdrop" role="presentation" onClick={busy ? undefined : onCancel}>
      <div className="sheet" role="dialog" aria-modal="true" aria-label={title} onClick={(e) => e.stopPropagation()}>
        <h2>{title}</h2>
        <div className="sheet-body">{children}</div>
        <div className="btn-row">
          <button type="button" className="btn btn-ghost" onClick={onCancel} disabled={busy}>취소</button>
          <button type="button" className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`} onClick={onConfirm} disabled={busy}>
            {busy ? '처리 중...' : confirmText}
          </button>
        </div>
      </div>
    </div>
  );
}

export function SocialButtons() {
  return (
    <div className="social">
      <a className="btn btn-social btn-google" href="/oauth2/authorization/google">
        <img src={logoUrl('google.com')} alt="" width="20" height="20" onError={(e) => { e.currentTarget.style.display = 'none'; }} />
        Google로 계속하기
      </a>
      <a className="btn btn-social btn-kakao" href="/oauth2/authorization/kakao">
        <svg width="20" height="20" viewBox="0 0 24 24" aria-hidden="true">
          <path fill="#191919" d="M12 3C6.5 3 2 6.5 2 10.8c0 2.8 1.9 5.2 4.7 6.6l-1 3.7c-.1.3.3.6.6.4l4.4-2.9c.4 0 .9.1 1.3.1 5.5 0 10-3.5 10-7.9S17.5 3 12 3z" />
        </svg>
        카카오로 계속하기
      </a>
    </div>
  );
}

/** 구독 서비스 로고. 알려진 서비스면 회사 아이콘, 아니면(또는 불러오기 실패 시) 첫 글자. */
export function ServiceLogo({ name, size = '' }) {
  const service = findService(name);
  const [failed, setFailed] = useState(false);
  const className = `logo${size ? ` logo-${size}` : ''}`;

  if (service && !failed) {
    return (
      <span className={className} aria-hidden="true">
        <img src={logoUrl(service.domain)} alt="" loading="lazy" onError={() => setFailed(true)} />
      </span>
    );
  }
  return (
    <span className={`${className} logo-initial`} style={initialStyle(name)} aria-hidden="true">
      {initialOf(name)}
    </span>
  );
}

/** 빈 화면. 자는 펭귄 그림과 안내 문구 */
export function EmptyState({ children, action }) {
  return (
    <div className="card empty">
      <img src="/images/sleep.webp" alt="" width="880" height="745" />
      <p>{children}</p>
      {action}
    </div>
  );
}

export function BackLink({ to, children }) {
  return <Link to={to} className="btn btn-ghost btn-block">{children}</Link>;
}
