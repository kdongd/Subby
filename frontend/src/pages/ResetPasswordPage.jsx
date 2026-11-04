import { useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import { api } from '../api.js';
import { Alert, Field } from '../components/Common.jsx';
import AuthLayout from './AuthLayout.jsx';

const PASSWORD_RULE = /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/;

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [form, setForm] = useState({ password: '', confirm: '' });
  const [error, setError] = useState(token ? '' : '링크가 올바르지 않습니다. 비밀번호 찾기를 다시 진행해 주세요.');
  const [done, setDone] = useState(false);
  const [busy, setBusy] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    if (!PASSWORD_RULE.test(form.password)) {
      setError('비밀번호는 8자 이상이며 영문과 숫자를 포함해야 합니다.');
      return;
    }
    if (form.password !== form.confirm) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      await api('/api/auth/password-reset/confirm', {
        method: 'POST',
        body: { token, newPassword: form.password },
        skipAuthRedirect: true,
      });
      setDone(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthLayout title="새 비밀번호 설정">
      {done ? (
        <>
          <Alert type="success">비밀번호를 바꿨습니다. 다른 기기의 로그인은 모두 해제되었습니다.</Alert>
          <Link to="/login" className="btn btn-primary btn-block">로그인하기</Link>
        </>
      ) : (
        <>
          <Alert>{error}</Alert>
          {token && (
            <form onSubmit={onSubmit} className="form">
              <Field label="새 비밀번호" hint="8자 이상, 영문과 숫자 포함">
                <input type="password" autoComplete="new-password" required value={form.password}
                       onChange={(e) => setForm({ ...form, password: e.target.value })} />
              </Field>
              <Field label="새 비밀번호 확인">
                <input type="password" autoComplete="new-password" required value={form.confirm}
                       onChange={(e) => setForm({ ...form, confirm: e.target.value })} />
              </Field>
              <button type="submit" className="btn btn-primary btn-block" disabled={busy}>
                {busy ? '저장 중...' : '비밀번호 바꾸기'}
              </button>
            </form>
          )}
          <div className="auth-links">
            <Link to="/forgot-password">링크 다시 받기</Link>
          </div>
        </>
      )}
    </AuthLayout>
  );
}
