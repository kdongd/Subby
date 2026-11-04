import { useState } from 'react';
import { Link } from 'react-router';
import { api } from '../api.js';
import { Alert, Field } from '../components/Common.jsx';
import AuthLayout from './AuthLayout.jsx';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await api('/api/auth/password-reset/request', { method: 'POST', body: { email }, skipAuthRedirect: true });
      setSent(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthLayout title="비밀번호 찾기" subtitle="가입한 이메일로 비밀번호 재설정 링크를 보내드립니다.">
      {sent ? (
        <>
          <Alert type="success">
            메일을 보냈습니다. 받은편지함을 확인해 주세요. 링크는 30분 동안 유효합니다.
          </Alert>
          <p className="muted small">메일이 보이지 않으면 스팸함도 확인해 주세요.</p>
        </>
      ) : (
        <>
          <Alert>{error}</Alert>
          <form onSubmit={onSubmit} className="form">
            <Field label="이메일">
              <input type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
            </Field>
            <button type="submit" className="btn btn-primary btn-block" disabled={busy}>
              {busy ? '보내는 중...' : '재설정 링크 받기'}
            </button>
          </form>
        </>
      )}
      <div className="auth-links">
        <Link to="/login">로그인으로 돌아가기</Link>
      </div>
    </AuthLayout>
  );
}
