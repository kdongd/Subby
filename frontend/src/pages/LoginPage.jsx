import { useState } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router';
import { useAuth } from '../auth.jsx';
import { Alert, Field, SocialButtons } from '../components/Common.jsx';
import { socialErrorMessage } from '../format.js';
import AuthLayout from './AuthLayout.jsx';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const socialError = searchParams.get('error');

  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState(socialError ? socialErrorMessage(socialError) : '');
  const [busy, setBusy] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await login(form.email, form.password);
      navigate(location.state?.from || '/', { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthLayout title="로그인" subtitle="구독을 챙겨주는 작은 비서">
      <Alert>{error}</Alert>
      <form onSubmit={onSubmit} className="form">
        <Field label="이메일">
          <input type="email" autoComplete="email" required value={form.email}
                 onChange={(e) => setForm({ ...form, email: e.target.value })} />
        </Field>
        <Field label="비밀번호">
          <input type="password" autoComplete="current-password" required value={form.password}
                 onChange={(e) => setForm({ ...form, password: e.target.value })} />
        </Field>
        <button type="submit" className="btn btn-primary btn-block" disabled={busy}>
          {busy ? '로그인 중...' : '로그인'}
        </button>
      </form>
      <div className="divider"><span>또는</span></div>
      <SocialButtons />
      <div className="auth-links">
        <Link to="/signup">회원가입</Link>
        <Link to="/forgot-password">비밀번호 찾기</Link>
      </div>
    </AuthLayout>
  );
}
