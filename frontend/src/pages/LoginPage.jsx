import { Link, useSearchParams } from 'react-router';
import { Alert, SocialButtons, SplashArt } from '../components/Common.jsx';
import { socialErrorMessage } from '../format.js';

// 첫 화면. 로그인은 Google·카카오만 쓰고, 처음 로그인하면 자동으로 가입된다.
// 이메일 + 비밀번호 로그인 API는 서버에 남아 있지만 화면에서는 쓰지 않는다.
export default function LoginPage() {
  const [searchParams] = useSearchParams();
  const socialError = searchParams.get('error');

  return (
    <div className="app">
      <div className="splash">
        <SplashArt />
        <div className="splash-sheet">
          <h2>펭귄 마을에 오신 걸 환영해요</h2>
          <Alert>{socialError ? socialErrorMessage(socialError) : ''}</Alert>
          <SocialButtons />
          <p className="note">처음이면 자동으로 가입돼요</p>
          <p className="note"><Link to="/privacy" className="muted">개인정보처리방침</Link></p>
        </div>
      </div>
    </div>
  );
}
