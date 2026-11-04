import { useEffect, useState } from 'react';
import { Navigate, useLocation } from 'react-router';
import { useAuth } from '../auth.jsx';

/** 로딩 표시. 무료 서버가 잠들어 있으면 첫 응답에 최대 1분이 걸리므로 3초가 지나면 안내를 덧붙인다. */
export function Loading({ text = '불러오는 중...' }) {
  const [slow, setSlow] = useState(false);
  useEffect(() => {
    const timer = setTimeout(() => setSlow(true), 3000);
    return () => clearTimeout(timer);
  }, []);
  return (
    <div className="loading">
      <div className="spinner" aria-hidden="true" />
      <p>{text}</p>
      {slow && <p className="muted small">서버를 깨우는 중입니다. 첫 접속은 최대 1분 정도 걸릴 수 있어요.</p>}
    </div>
  );
}

export function RequireAuth({ children }) {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) return <Loading />;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return children;
}

export function GuestOnly({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <Loading />;
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

/** 확인 창. 삭제·해지처럼 되돌리기 어려운 동작 전에 띄운다. */
export function ConfirmDialog({ title, children, confirmText, danger, onConfirm, onCancel, busy }) {
  return (
    <div className="dialog-backdrop" role="presentation" onClick={onCancel}>
      <div className="dialog" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
        <h3>{title}</h3>
        <div className="dialog-body">{children}</div>
        <div className="dialog-actions">
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
      <a className="btn btn-social btn-google" href="/oauth2/authorization/google">Google로 계속하기</a>
      <a className="btn btn-social btn-kakao" href="/oauth2/authorization/kakao">카카오로 계속하기</a>
    </div>
  );
}
