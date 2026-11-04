import { useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import { api } from '../api.js';
import { Alert } from '../components/Common.jsx';
import AuthLayout from './AuthLayout.jsx';

// 메일 링크를 여는 것만으로는 알림이 꺼지지 않고, 버튼을 눌러야 꺼진다.
// 메일 보안 프로그램이 링크를 미리 열어보는 경우를 막기 위해서다.
export default function UnsubscribePage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [done, setDone] = useState(false);
  const [error, setError] = useState(token ? '' : '링크가 올바르지 않습니다.');
  const [busy, setBusy] = useState(false);

  const onClick = async () => {
    setBusy(true);
    setError('');
    try {
      await api('/api/notifications/unsubscribe', { method: 'POST', body: { token }, skipAuthRedirect: true });
      setDone(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthLayout title="결제 알림 끄기">
      {done ? (
        <Alert type="success">결제 알림을 껐습니다. 설정 화면에서 언제든 다시 켤 수 있습니다.</Alert>
      ) : (
        <>
          <Alert>{error}</Alert>
          {token && (
            <>
              <p>더 이상 결제 예정 메일을 받지 않으려면 아래 버튼을 눌러 주세요.</p>
              <button type="button" className="btn btn-primary btn-block" onClick={onClick} disabled={busy}>
                {busy ? '처리 중...' : '알림 끄기'}
              </button>
            </>
          )}
        </>
      )}
      <div className="auth-links">
        <Link to="/">서비로 가기</Link>
      </div>
    </AuthLayout>
  );
}
