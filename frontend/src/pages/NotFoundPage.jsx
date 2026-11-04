import { Link } from 'react-router';
import AuthLayout from './AuthLayout.jsx';

export default function NotFoundPage() {
  return (
    <AuthLayout title="페이지를 찾을 수 없습니다">
      <Link to="/" className="btn btn-primary btn-block">처음으로</Link>
    </AuthLayout>
  );
}
