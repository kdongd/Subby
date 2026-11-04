import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { api } from '../api.js';
import { Alert, Loading } from '../components/Common.jsx';
import { amount, CATEGORIES, CATEGORY_LABEL, CYCLE_LABEL, krw, shortDate } from '../format.js';

export default function SubscriptionListPage() {
  const [status, setStatus] = useState('ACTIVE');
  const [category, setCategory] = useState('');
  const [items, setItems] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    setItems(null);
    setError('');
    api('/api/subscriptions', { params: { status, category } })
      .then(setItems)
      .catch((err) => setError(err.message));
  }, [status, category]);

  return (
    <div className="stack">
      <div className="page-head">
        <h1>구독 목록</h1>
        <Link to="/subscriptions/new" className="btn btn-primary">구독 등록</Link>
      </div>

      <div className="filters">
        <div className="segmented" role="group" aria-label="상태">
          {[['ACTIVE', '이용 중'], ['CANCELED', '해지'], ['', '전체']].map(([value, label]) => (
            <button key={label} type="button" className={status === value ? 'active' : ''} onClick={() => setStatus(value)}>
              {label}
            </button>
          ))}
        </div>
        <select value={category} onChange={(e) => setCategory(e.target.value)} aria-label="카테고리">
          <option value="">모든 카테고리</option>
          {CATEGORIES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
      </div>

      <Alert>{error}</Alert>
      {!items && !error && <Loading />}
      {items && items.length === 0 && (
        <div className="card empty">
          <p>조건에 맞는 구독이 없습니다.</p>
          <Link to="/subscriptions/new" className="btn btn-primary">첫 구독 등록하기</Link>
        </div>
      )}
      {items && items.length > 0 && (
        <ul className="list card">
          {items.map((s) => (
            <li key={s.id}>
              <Link to={`/subscriptions/${s.id}`} className="list-row">
                <span className="list-main">
                  <strong>{s.name} {s.status === 'CANCELED' && <span className="badge">해지</span>}</strong>
                  <span className="muted small">
                    {CATEGORY_LABEL[s.category]} · {CYCLE_LABEL[s.billingCycle]}
                    {s.nextPaymentDate ? ` · 다음 결제 ${shortDate(s.nextPaymentDate)}` : ''}
                  </span>
                </span>
                <span className="list-amount">
                  {amount(s.currency, s.amount)}
                  {s.currency === 'USD' && <span className="muted small">약 {krw(s.amountKrw)}</span>}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
