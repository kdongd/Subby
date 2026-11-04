import { useEffect, useState } from 'react';
import { Link, useOutletContext } from 'react-router';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Alert, Loading, ServiceLogo } from '../components/Common.jsx';
import CategoryBars from '../components/CategoryBars.jsx';
import ExchangeRateNote from '../components/ExchangeRateNote.jsx';
import { BellIcon, CalendarIcon, ChartIcon, ChevronIcon, ListIcon, PlusIcon } from '../components/Icons.jsx';
import { amount, dDayLabel, krw, shortDate } from '../format.js';

export default function DashboardPage() {
  const { user } = useAuth();
  const { unread } = useOutletContext();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api('/api/dashboard').then(setData).catch((err) => setError(err.message));
  }, []);

  return (
    <div className="page">
      <header className="home-head">
        <div className="home-greeting">
          <span className="brand">서비</span>
          <p>{user?.nickname}님<br />안녕하세요!</p>
        </div>
        <img className="home-peek" src="/images/peek.webp" alt="" width="874" height="648" />
        <Link to="/notifications" className="icon-btn" aria-label={unread > 0 ? `알림, 안 읽은 알림 ${unread}개` : '알림'}>
          <BellIcon />
          {unread > 0 && <span className="dot-badge">{unread > 99 ? '99+' : unread}</span>}
        </Link>
      </header>

      <Alert>{error}</Alert>
      {!data && !error && <Loading />}
      {data && (
        <>
          <section className="hero" aria-label="이번 달 구독료">
            <span className="hero-label">{Number(data.yearMonth.slice(5))}월 구독료</span>
            <strong className="hero-amount num">{krw(data.thisMonthTotal)}</strong>
            <div className="hero-meta">
              <span>남은 결제 {krw(data.thisMonthRemaining)}</span>
              <span>이용 중 {data.activeCount}개</span>
            </div>
          </section>

          <section className="section">
            <div className="card due-card">
              <p>
                {data.upcoming.length > 0
                  ? <>다가오는 결제<br /><span className="count">{data.upcoming.length}건</span>이 있어요!</>
                  : <>7일 안에 결제될<br />구독이 없어요</>}
              </p>
              <span className="cal-icon"><CalendarIcon size={26} /></span>
            </div>
            {data.upcoming.length > 0 && (
              <ul className="tile-list">
                {data.upcoming.map((p) => (
                  <li key={`${p.subscriptionId}-${p.paymentDate}`}>
                    <Link to={`/subscriptions/${p.subscriptionId}`} className="tile">
                      <ServiceLogo name={p.name} />
                      <span className="row-main">
                        <strong>{p.name}</strong>
                        <span className="muted small">{shortDate(p.paymentDate)} · {amount(p.currency, p.amount)}</span>
                      </span>
                      <span className={`chip ${p.dDay <= 1 ? 'chip-warn' : ''}`}>{dDayLabel(p.dDay)}</span>
                      <ChevronIcon className="chev" />
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <nav className="quick" aria-label="바로가기">
            <Link to="/subscriptions/new">
              <span className="quick-icon" style={{ background: '#e3edfd', color: '#3d74e0' }}><PlusIcon size={22} /></span>
              구독 추가
            </Link>
            <Link to="/subscriptions">
              <span className="quick-icon" style={{ background: '#e6f6ee', color: '#1e7a46' }}><ListIcon size={22} /></span>
              구독 목록
            </Link>
            <Link to="/stats">
              <span className="quick-icon" style={{ background: '#fff0d6', color: '#a35c00' }}><ChartIcon size={22} /></span>
              지출 통계
            </Link>
          </nav>

          <section className="card section">
            <h2>이번 달 어디에 쓰고 있나요</h2>
            <CategoryBars items={data.categories} />
          </section>

          <p className="note">월 평균 약 {krw(data.monthlyAverage)} (연간·주간 결제를 월 단위로 환산)</p>
          <ExchangeRateNote info={data.exchangeRate} />
        </>
      )}
    </div>
  );
}
