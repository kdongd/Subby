import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router';
import { api } from '../api.js';
import { Alert, BackLink, ConfirmDialog, Field, Loading, PageHeader, ServiceLogo } from '../components/Common.jsx';
import SubscriptionForm from '../components/SubscriptionForm.jsx';
import {
  amount, CATEGORY_LABEL, CYCLE_LABEL, CYCLE_PREFIX, daysUntil, dDayLabel, fullDate, krw, shortDate, todayIso,
} from '../format.js';

export default function SubscriptionDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [item, setItem] = useState(null);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(false);
  const [dialog, setDialog] = useState(null); // 'cancel' | 'delete'
  const [canceledAt, setCanceledAt] = useState(todayIso());
  const [dialogError, setDialogError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api(`/api/subscriptions/${id}`).then(setItem).catch((err) => setError(err.message));
  }, [id]);

  if (error) {
    return (
      <div className="page">
        <PageHeader title="구독" back="/subscriptions" />
        <Alert>{error}</Alert>
        <BackLink to="/subscriptions">목록으로</BackLink>
      </div>
    );
  }
  if (!item) {
    return (
      <div className="page">
        <PageHeader title="구독" back="/subscriptions" />
        <Loading />
      </div>
    );
  }

  const onUpdate = async (form) => {
    const updated = await api(`/api/subscriptions/${id}`, { method: 'PUT', body: form });
    setItem(updated);
    setEditing(false);
  };

  const onCancelSubscription = async () => {
    setBusy(true);
    setDialogError('');
    try {
      const updated = await api(`/api/subscriptions/${id}/cancel`, { method: 'POST', body: { canceledAt } });
      setItem(updated);
      setDialog(null);
    } catch (err) {
      setDialogError(err.fieldErrors?.canceledAt || err.message);
    } finally {
      setBusy(false);
    }
  };

  const onDelete = async () => {
    setBusy(true);
    setDialogError('');
    try {
      await api(`/api/subscriptions/${id}`, { method: 'DELETE' });
      navigate('/subscriptions', { replace: true });
    } catch (err) {
      setDialogError(err.message);
      setBusy(false);
    }
  };

  const closeDialog = () => {
    setDialog(null);
    setDialogError('');
  };

  if (editing) {
    return (
      <div className="page">
        <header className="page-header">
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => setEditing(false)}>취소</button>
          <h1 className="center">구독 수정</h1>
          <span style={{ width: 56 }} aria-hidden="true" />
        </header>
        <SubscriptionForm editing initial={{ ...item, amount: String(item.amount) }} submitText="저장하기" onSubmit={onUpdate} />
      </div>
    );
  }

  const active = item.status === 'ACTIVE';
  const nextDays = item.nextPaymentDate ? daysUntil(item.nextPaymentDate) : null;

  return (
    <div className="page">
      <PageHeader title="구독 정보" back="/subscriptions" />

      <section className="card detail-hero">
        <ServiceLogo name={item.name} size="lg" />
        <h1>{item.name}</h1>
        <strong className="detail-amount num">
          {CYCLE_PREFIX[item.billingCycle]} {amount(item.currency, item.amount)}
        </strong>
        {item.currency === 'USD' && <span className="muted small">약 {krw(item.amountKrw)} (최신 환율 기준)</span>}
        {active
          ? nextDays !== null && <span className={`chip ${nextDays <= 1 ? 'chip-warn' : ''}`}>다음 결제 {dDayLabel(nextDays)}</span>
          : <span className="chip chip-gray">{fullDate(item.canceledAt)} 해지</span>}
      </section>

      <section className="card">
        <dl className="kv">
          <dt>카테고리</dt>
          <dd>{CATEGORY_LABEL[item.category]}</dd>
          <dt>결제 주기</dt>
          <dd>{CYCLE_LABEL[item.billingCycle]}</dd>
          <dt>결제 기준일</dt>
          <dd>{fullDate(item.startDate)}</dd>
          {item.memo && (
            <>
              <dt>메모</dt>
              <dd style={{ whiteSpace: 'pre-line' }}>{item.memo}</dd>
            </>
          )}
        </dl>
      </section>

      <section className="card section">
        <h2>다음 결제일</h2>
        {item.upcomingPayments.length === 0 ? (
          <p className="muted">예정된 결제가 없어요.</p>
        ) : (
          <ol className="timeline">
            {item.upcomingPayments.map((date) => (
              <li key={date}>
                <span>{shortDate(date)}</span>
                <span className="muted small">{dDayLabel(daysUntil(date))}</span>
              </li>
            ))}
          </ol>
        )}
      </section>

      <div className="btn-row">
        <button type="button" className="btn btn-ghost" onClick={() => setEditing(true)}>수정</button>
        {active && <button type="button" className="btn btn-ghost" onClick={() => setDialog('cancel')}>해지</button>}
        <button type="button" className="btn btn-danger-ghost" onClick={() => setDialog('delete')}>삭제</button>
      </div>

      {dialog === 'cancel' && (
        <ConfirmDialog title="구독 해지" confirmText="해지하기" onConfirm={onCancelSubscription} onCancel={closeDialog} busy={busy}>
          <p>해지일 당일과 그 이후 결제는 계산하지 않아요. 해지 전 결제 기록은 통계에 남아요.</p>
          <Field label="해지일" error={dialogError}>
            <input type="date" value={canceledAt} onChange={(e) => setCanceledAt(e.target.value)} />
          </Field>
        </ConfirmDialog>
      )}
      {dialog === 'delete' && (
        <ConfirmDialog title="구독 삭제" confirmText="삭제하기" danger onConfirm={onDelete} onCancel={closeDialog} busy={busy}>
          <p>잘못 등록한 구독을 완전히 지워요. <strong>통계에서도 사라져요.</strong></p>
          <p className="small">그만 쓰는 구독이라면 삭제 대신 해지를 골라 주세요.</p>
          <Alert>{dialogError}</Alert>
        </ConfirmDialog>
      )}
    </div>
  );
}
