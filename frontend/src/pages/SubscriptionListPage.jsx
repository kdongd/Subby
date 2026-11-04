import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router';
import { api } from '../api.js';
import { Alert, EmptyState, Loading, PageHeader, ServiceLogo } from '../components/Common.jsx';
import { ChevronIcon, PlusIcon } from '../components/Icons.jsx';
import { amount, CATEGORIES, CYCLE_PREFIX, daysUntil, dDayLabel, krw } from '../format.js';

const TABS = [
  ['', '전체'],
  ['ACTIVE', '이용 중'],
  ['CANCELED', '해지'],
];

export default function SubscriptionListPage() {
  const [items, setItems] = useState(null);
  const [error, setError] = useState('');
  const [status, setStatus] = useState('');
  const [category, setCategory] = useState('');

  // 탭마다 개수를 보여주기 위해 한 번에 전부 받아서 화면에서 나눈다(사용자당 구독 수는 많지 않다).
  useEffect(() => {
    api('/api/subscriptions').then(setItems).catch((err) => setError(err.message));
  }, []);

  const counts = useMemo(() => ({
    '': items?.length ?? 0,
    ACTIVE: items?.filter((s) => s.status === 'ACTIVE').length ?? 0,
    CANCELED: items?.filter((s) => s.status === 'CANCELED').length ?? 0,
  }), [items]);

  const usedCategories = useMemo(
    () => CATEGORIES.filter(([value]) => items?.some((s) => s.category === value)),
    [items],
  );

  const visible = useMemo(() => (items || [])
    .filter((s) => !status || s.status === status)
    .filter((s) => !category || s.category === category)
    .sort((a, b) => {
      // 이용 중인 구독을 다음 결제일이 가까운 순서로, 해지한 구독은 뒤로
      if (a.status !== b.status) return a.status === 'ACTIVE' ? -1 : 1;
      return (a.nextPaymentDate || '9999').localeCompare(b.nextPaymentDate || '9999');
    }), [items, status, category]);

  return (
    <div className="page">
      <PageHeader title="구독 목록" />

      <div className="segmented" role="tablist" aria-label="구독 상태">
        {TABS.map(([value, label]) => (
          <button key={label} type="button" role="tab" aria-selected={status === value}
                  className={status === value ? 'active' : ''} onClick={() => setStatus(value)}>
            {label} ({counts[value]})
          </button>
        ))}
      </div>

      {usedCategories.length > 1 && (
        <div className="chips" role="group" aria-label="카테고리">
          <button type="button" className={`chip-btn ${category === '' ? 'active' : ''}`} onClick={() => setCategory('')}>모두</button>
          {usedCategories.map(([value, label]) => (
            <button key={value} type="button" className={`chip-btn ${category === value ? 'active' : ''}`}
                    aria-pressed={category === value} onClick={() => setCategory(value)}>
              {label}
            </button>
          ))}
        </div>
      )}

      <Alert>{error}</Alert>
      {!items && !error && <Loading />}
      {items && items.length === 0 && (
        <EmptyState action={<Link to="/subscriptions/new" className="btn btn-primary">첫 구독 등록하기</Link>}>
          아직 등록한 구독이 없어요.<br />쓰고 있는 구독을 하나 넣어 볼까요?
        </EmptyState>
      )}
      {items && items.length > 0 && visible.length === 0 && (
        <EmptyState>조건에 맞는 구독이 없어요.</EmptyState>
      )}
      {visible.length > 0 && (
        <ul className="tile-list">
          {visible.map((s) => <SubscriptionTile key={s.id} item={s} />)}
        </ul>
      )}

      <Link to="/subscriptions/new" className="fab" aria-label="구독 추가">
        <PlusIcon size={28} />
      </Link>
    </div>
  );
}

function SubscriptionTile({ item }) {
  const canceled = item.status === 'CANCELED';
  const days = item.nextPaymentDate ? daysUntil(item.nextPaymentDate) : null;
  return (
    <li>
      <Link to={`/subscriptions/${item.id}`} className="tile" style={canceled ? { opacity: 0.7 } : undefined}>
        <ServiceLogo name={item.name} />
        <span className="row-main">
          <strong>{item.name}</strong>
          <span className="muted small num">
            {CYCLE_PREFIX[item.billingCycle]} {amount(item.currency, item.amount)}
            {item.currency === 'USD' && ` · 약 ${krw(item.amountKrw)}`}
          </span>
        </span>
        {canceled && <span className="chip chip-gray">해지</span>}
        {!canceled && days !== null && (
          <span className={`chip ${days <= 1 ? 'chip-warn' : days <= 7 ? 'chip-danger' : ''}`}>{dDayLabel(days)}</span>
        )}
        <ChevronIcon className="chev" />
      </Link>
    </li>
  );
}
