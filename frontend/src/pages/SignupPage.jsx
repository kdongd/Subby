import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { useAuth } from '../auth.jsx';
import { Alert, Field, SocialButtons } from '../components/Common.jsx';
import AuthLayout from './AuthLayout.jsx';

const PASSWORD_RULE = /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/;

export default function SignupPage() {
  const { signup } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '', passwordConfirm: '', nickname: '' });
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const validate = () => {
    const errors = {};
    if (!PASSWORD_RULE.test(form.password)) errors.password = '비밀번호는 8자 이상이며 영문과 숫자를 포함해야 합니다.';
    if (form.password !== form.passwordConfirm) errors.passwordConfirm = '비밀번호가 일치하지 않습니다.';
    if (!form.nickname.trim()) errors.nickname = '닉네임을 입력해 주세요.';
    return errors;
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length) return;
    setBusy(true);
    setError('');
    try {
      await signup({ email: form.email, password: form.password, nickname: form.nickname.trim() });
      navigate('/', { replace: true });
    } catch (err) {
      setFieldErrors(err.fieldErrors || {});
      if (!Object.keys(err.fieldErrors || {}).length) setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthLayout title="회원가입" subtitle="이메일로 가입하거나 소셜 계정으로 바로 시작하세요.">
      <Alert>{error}</Alert>
      <form onSubmit={onSubmit} className="form" noValidate>
        <Field label="이메일" error={fieldErrors.email}>
          <input type="email" autoComplete="email" required value={form.email} onChange={set('email')} />
        </Field>
        <Field label="비밀번호" error={fieldErrors.password} hint="8자 이상, 영문과 숫자 포함">
          <input type="password" autoComplete="new-password" required value={form.password} onChange={set('password')} />
        </Field>
        <Field label="비밀번호 확인" error={fieldErrors.passwordConfirm}>
          <input type="password" autoComplete="new-password" required value={form.passwordConfirm} onChange={set('passwordConfirm')} />
        </Field>
        <Field label="닉네임" error={fieldErrors.nickname}>
          <input type="text" maxLength={30} required value={form.nickname} onChange={set('nickname')} />
        </Field>
        <button type="submit" className="btn btn-primary btn-block" disabled={busy}>
          {busy ? '가입 중...' : '가입하기'}
        </button>
      </form>
      <div className="divider"><span>또는</span></div>
      <SocialButtons />
      <div className="auth-links">
        <span className="muted">이미 계정이 있나요?</span>
        <Link to="/login">로그인</Link>
      </div>
    </AuthLayout>
  );
}
