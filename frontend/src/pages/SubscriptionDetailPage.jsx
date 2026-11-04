import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { api } from '../api.js';
import { Alert, ConfirmDialog, Field, Loading } from '../components/Common.jsx';
import SubscriptionForm from '../components/SubscriptionForm.jsx';
import { amount, CATEGORY_LABEL, CYCLE_LABEL, fullDate, krw, shortDate, todayIso } from '../format.js';

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
      <div className="stack narrow">
        <Alert>{error}</Alert>
        <Link to="/subscriptions" className="btn btn-ghost">목록으로</Link>
      </div>
    );
  }
  if (!item) return <Loading />;

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
      <div className="stack narrow">
        <h1>구독 수정</h1>
        <SubscriptionForm
          initial={{ ...item, amount: String(item.amount) }}
          submitText="저장"
          onSubmit={onUpdate}
          onCancel={() => setEditing(false)}
        />
      </div>
    );
  }

  const active = item.status === 'ACTIVE';

  return (
    <div className="stack narrow">
      <Link to="/subscriptions" className="back">← 구독 목록</Link>
      <div className="page-head">
        <h1>{item.name} {!active && <span className="badge">해지</span>}</h1>
      </div>

      <section className="card">
        <dl className="details">
          <dt>금액</dt>
          <dd>
            {amount(item.currency, item.amount)}
            {item.currency === 'USD' && <span className="muted small"> (약 {krw(item.amountKrw)}, 최신 환율 기준)</span>}
          </dd>
          <dt>결제 주기</dt>
          <dd>{CYCLE_LABEL[item.billingCycle]}</dd>
          <dt>카테고리</dt>
          <dd>{CATEGORY_LABEL[item.category]}</dd>
          <dt>첫 결제일</dt>
          <dd>{fullDate(item.startDate)}</dd>
          {!active && (
            <>
              <dt>해지일</dt>
              <dd>{fullDate(item.canceledAt)}</dd>
            </>
          )}
          {item.memo && (
            <>
              <dt>메모</dt>
              <dd>{item.memo}</dd>
            </>
          )}
        </dl>
      </section>

      <section className="card">
        <h2>다음 결제일</h2>
        {item.upcomingPayments.length === 0 ? (
          <p className="muted">예정된 결제가 없습니다.</p>
        ) : (
          <ol className="upcoming">
            {item.upcomingPayments.map((date) => <li key={date}>{shortDate(date)}</li>)}
          </ol>
        )}
      </section>

      <div className="form-actions">
        <button type="button" className="btn btn-ghost" onClick={() => setEditing(true)}>수정</button>
        {active && <button type="button" className="btn btn-ghost" onClick={() => setDialog('cancel')}>해지</button>}
        <button type="button" className="btn btn-danger" onClick={() => setDialog('delete')}>삭제</button>
      </div>

      {dialog === 'cancel' && (
        <ConfirmDialog title="구독 해지" confirmText="해지" onConfirm={onCancelSubscription} onCancel={closeDialog} busy={busy}>
          <p>해지일 당일과 그 이후 결제는 계산하지 않습니다. 해지 전 결제 기록은 통계에 남습니다.</p>
          <Field label="해지일" error={dialogError}>
            <input type="date" value={canceledAt} onChange={(e) => setCanceledAt(e.target.value)} />
          </Field>
        </ConfirmDialog>
      )}
      {dialog === 'delete' && (
        <ConfirmDialog title="구독 삭제" confirmText="삭제" danger onConfirm={onDelete} onCancel={closeDialog} busy={busy}>
          <p>잘못 등록한 구독을 완전히 지웁니다. <strong>통계에서도 사라집니다.</strong></p>
          <p className="muted small">그만 쓰는 구독이라면 삭제 대신 해지를 선택하세요.</p>
          <Alert>{dialogError}</Alert>
        </ConfirmDialog>
      )}
    </div>
  );
}
