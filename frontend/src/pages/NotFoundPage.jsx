import { Link } from 'react-router';
import { EmptyState } from '../components/Common.jsx';

export default function NotFoundPage() {
  return (
    <div className="app">
      <div className="page page-plain" style={{ paddingTop: 80 }}>
        <EmptyState action={<Link to="/" className="btn btn-primary">처음으로</Link>}>
          찾는 화면이 없어요.<br />서비가 잠깐 졸았나 봐요.
        </EmptyState>
      </div>
    </div>
  );
}
