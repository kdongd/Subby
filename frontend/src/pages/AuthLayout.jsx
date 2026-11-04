import { Link } from 'react-router';

export default function AuthLayout({ title, subtitle, children }) {
  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-brand">서비</div>
        <h1>{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
        {children}
        <p className="auth-footer">
          <Link to="/privacy">개인정보처리방침</Link>
        </p>
      </div>
    </div>
  );
}
