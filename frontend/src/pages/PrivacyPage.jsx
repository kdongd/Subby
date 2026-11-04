import { useNavigate } from 'react-router';
import { BackIcon } from '../components/Icons.jsx';

// 개인정보처리방침. 로그인 없이 누구나 볼 수 있어야 한다 (Google, 카카오 로그인 심사에서 이 주소를 확인함).
const EFFECTIVE_DATE = '2026년 11월 4일';
const CONTACT_URL = 'https://github.com/kdongd/Subby/issues';

export default function PrivacyPage() {
  const navigate = useNavigate();
  const goBack = () => (window.history.state?.idx > 0 ? navigate(-1) : navigate('/', { replace: true }));

  return (
    <div className="app">
      <div className="page page-plain">
        <header className="page-header">
          <button type="button" className="icon-btn plain" onClick={goBack} aria-label="뒤로"><BackIcon /></button>
          <h1 className="center">개인정보처리방침</h1>
          <span style={{ width: 44 }} aria-hidden="true" />
        </header>

        <article className="card doc">
          <p className="muted small">시행일: {EFFECTIVE_DATE}</p>
          <p>
            서비(Subby, 이하 "서비스")는 이용자의 개인정보를 소중히 다루며, 「개인정보 보호법」 등 관련 법령을 지킵니다.
            이 방침은 서비스가 어떤 정보를 왜 수집하고, 어떻게 보관하고 지우는지 설명합니다.
          </p>

          <h2>1. 수집하는 개인정보</h2>
          <div className="doc-table-wrap">
            <table className="doc-table">
              <thead>
                <tr><th>구분</th><th>항목</th></tr>
              </thead>
              <tbody>
                <tr><td>Google 로그인</td><td>Google 계정 고유 식별자, 이메일, 이름(닉네임으로 사용)</td></tr>
                <tr><td>카카오 로그인</td><td>카카오 계정 고유 식별자, 닉네임, 이메일(이용자가 동의한 경우에만)</td></tr>
                <tr><td>서비스 이용 중 입력</td><td>구독 서비스명, 카테고리, 결제 금액과 통화, 결제 주기, 결제 기준일, 해지일, 메모</td></tr>
                <tr><td>푸시 알림을 켠 경우</td><td>브라우저가 발급한 푸시 구독 정보(푸시 서버 주소와 암호화 키), 브라우저 종류(User-Agent)</td></tr>
                <tr><td>자동 생성</td><td>로그인 세션 정보, 결제 알림 내역(사이트 안 알림)</td></tr>
              </tbody>
            </table>
          </div>
          <p className="muted small">서비스는 카드 번호, 계좌 번호 등 결제 수단 정보를 수집하지 않습니다.</p>

          <h2>2. 이용 목적</h2>
          <ul>
            <li>회원 식별, 로그인, 계정 관리</li>
            <li>구독 목록, 다음 결제일, 지출 통계 제공</li>
            <li>결제 예정 알림(사이트 안 알림, 기기 푸시 알림) 제공</li>
            <li>무단 접근 차단 등 보안 유지</li>
          </ul>

          <h2>3. 보관 기간과 파기</h2>
          <ul>
            <li>회원 정보와 구독 정보: 이용자가 삭제를 요청할 때까지 보관하며, 요청 시 지체 없이 파기합니다.</li>
            <li>구독 정보는 이용자가 서비스 안에서 직접 삭제하면 즉시 지워집니다.</li>
            <li>로그인 세션: 마지막 이용 후 30분이 지나면 만료됩니다.</li>
            <li>결제 알림 내역: 만든 날로부터 90일이 지나면 자동으로 지웁니다.</li>
            <li>푸시 구독 정보: 이용자가 기기 푸시를 끄거나 로그아웃하면 지우며, 브라우저가 구독을 해지한 사실을 확인하면 자동으로 지웁니다.</li>
          </ul>

          <h2>4. 제3자 제공</h2>
          <p>서비스는 이용자의 개인정보를 제3자에게 제공하지 않습니다. 법령에 따라 요구되는 경우는 예외입니다.</p>

          <h2>5. 처리 위탁 및 국외 이전</h2>
          <p>서비스 운영을 위해 아래 업체에 처리를 맡기고 있으며, 일부는 국외에서 처리됩니다.</p>
          <div className="doc-table-wrap">
            <table className="doc-table">
              <thead>
                <tr><th>업체</th><th>업무</th><th>위치</th></tr>
              </thead>
              <tbody>
                <tr><td>Render Services, Inc.</td><td>서버 운영</td><td>싱가포르</td></tr>
                <tr><td>Neon, Inc.</td><td>데이터베이스 보관</td><td>싱가포르</td></tr>
                <tr><td>Google LLC, (주)카카오</td><td>소셜 로그인 인증</td><td>각 사 정책에 따름</td></tr>
                <tr><td>Google LLC, Apple Inc., Mozilla Corporation, Microsoft Corporation</td><td>기기 푸시 알림 전달 (이용자가 쓰는 브라우저 회사 1곳만 해당). 알림 내용은 암호화되어 전달되며 해당 업체는 내용을 볼 수 없습니다.</td><td>각 사 정책에 따름</td></tr>
                <tr><td>Google LLC</td><td>구독 서비스 로고 이미지 제공. 화면에 로고를 표시할 때 이용자의 브라우저가 Google에 접속하며, 이때 접속 IP와 로고를 표시할 서비스의 사이트 주소(예: netflix.com)가 전달됩니다.</td><td>미국 등</td></tr>
              </tbody>
            </table>
          </div>

          <h2>6. 쿠키</h2>
          <p>
            서비스는 로그인 상태 유지(SESSION)와 위조 요청 방지(XSRF-TOKEN)를 위해 쿠키를 사용합니다.
            광고나 이용자 추적 목적의 쿠키는 사용하지 않습니다. 브라우저에서 쿠키를 차단하면 로그인할 수 없습니다.
          </p>

          <h2>7. 이용자의 권리</h2>
          <ul>
            <li>이용자는 언제든 자신의 개인정보 열람, 정정, 삭제, 처리 정지를 요청할 수 있습니다.</li>
            <li>닉네임과 구독 정보는 서비스 화면에서 직접 수정하거나 삭제할 수 있습니다.</li>
            <li>결제 알림과 기기 푸시는 내 정보 화면에서 끌 수 있습니다.</li>
            <li>소셜 로그인 연결은 내 정보 화면에서 해제할 수 있습니다.</li>
          </ul>

          <h2>8. 안전성 확보 조치</h2>
          <ul>
            <li>모든 통신은 HTTPS로 암호화합니다.</li>
            <li>푸시 알림 내용은 기기별 키로 암호화해 보냅니다.</li>
            <li>이용자는 자신의 정보에만 접근할 수 있도록 접근을 통제합니다.</li>
          </ul>

          <h2>9. 문의</h2>
          <p>
            개인정보 관련 문의, 열람·삭제 요청은 아래로 보내 주세요.
            <br />
            <a href={CONTACT_URL} target="_blank" rel="noreferrer">{CONTACT_URL}</a>
          </p>

          <h2>10. 방침 변경</h2>
          <p>이 방침이 바뀌면 시행 7일 전에 이 페이지에 알립니다.</p>
        </article>
      </div>
    </div>
  );
}
