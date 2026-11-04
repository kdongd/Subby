import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { api } from '../api.js';
import { Alert, Loading } from '../components/Common.jsx';
import ExchangeRateNote from '../components/ExchangeRateNote.jsx';
import CategoryBars from '../components/CategoryBars.jsx';
import { amount, CATEGORY_LABEL, dDay, krw, monthLabel, shortDate } from '../format.js';

export default function DashboardPage() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api('/api/dashboard').then(setData).catch((err) => setError(err.message));
  }, []);

  if (error) return <Alert>{error}</Alert>;
  if (!data) return <Loading />;

  return (
    <div className="stack">
      <div className="page-head">
        <h1>{monthLabel(data.yearMonth)} 구독 현황</h1>
        <Link to="/subscriptions/new" className="btn btn-primary">구독 등록</Link>
      </div>

      <div className="cards">
        <div className="card stat">
          <span className="stat-label">이번 달 결제 예정액</span>
          <strong className="stat-value">{krw(data.thisMonthTotal)}</strong>
        </div>
        <div className="card stat">
          <span className="stat-label">이번 달 남은 결제액</span>
          <strong className="stat-value">{krw(data.thisMonthRemaining)}</strong>
          <span className="stat-sub">오늘 포함</span>
        </div>
        <div className="card stat">
          <span className="stat-label">월 평균 환산액</span>
          <strong className="stat-value">약 {krw(data.monthlyAverage)}</strong>
          <span className="stat-sub">연간·주간 결제를 월 단위로 환산</span>
        </div>
        <div className="card stat">
          <span className="stat-label">활성 구독</span>
          <strong className="stat-value">{data.activeCount}개</strong>
        </div>
      </div>

      <section className="card">
        <h2>7일 안에 결제될 구독</h2>
        {data.upcoming.length === 0 ? (
          <p className="muted">7일 안에 결제될 구독이 없습니다.</p>
        ) : (
          <ul className="list">
            {data.upcoming.map((p) => (
              <li key={`${p.subscriptionId}-${p.paymentDate}`}>
                <Link to={`/subscriptions/${p.subscriptionId}`} className="list-row">
                  <span className={`badge ${p.dDay <= 1 ? 'badge-warn' : ''}`}>{dDay(p.dDay)}</span>
                  <span className="list-main">
                    <strong>{p.name}</strong>
                    <span className="muted small">{shortDate(p.paymentDate)} · {CATEGORY_LABEL[p.category]}</span>
                  </span>
                  <span className="list-amount">
                    {amount(p.currency, p.amount)}
                    {p.currency === 'USD' && <span className="muted small">약 {krw(p.amountKrw)}</span>}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="card">
        <h2>이번 달 카테고리별 결제액</h2>
        <CategoryBars items={data.categories} />
      </section>

      <ExchangeRateNote info={data.exchangeRate} />
    </div>
  );
}
