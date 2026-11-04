import { useState } from 'react';
import { Alert, Field } from './Common.jsx';
import { CATEGORIES, CURRENCIES, CYCLES, todayIso } from '../format.js';

const EMPTY = {
  name: '',
  category: 'OTT',
  currency: 'KRW',
  amount: '',
  billingCycle: 'MONTHLY',
  startDate: todayIso(),
  memo: '',
};

/** 등록·수정 공용 입력 폼 */
export default function SubscriptionForm({ initial, submitText, onSubmit, onCancel }) {
  const [form, setForm] = useState(() => ({ ...EMPTY, ...initial, memo: initial?.memo || '' }));
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const validate = () => {
    const errors = {};
    if (!form.name.trim()) errors.name = '서비스명을 입력해 주세요.';
    const value = Number(form.amount);
    if (form.amount === '' || Number.isNaN(value) || value <= 0) {
      errors.amount = '결제 금액을 입력해 주세요.';
    } else if (form.currency === 'KRW' && !Number.isInteger(value)) {
      errors.amount = '원화 금액은 정수로 입력해 주세요.';
    } else if (form.currency === 'USD' && !/^\d+(\.\d{1,2})?$/.test(String(form.amount))) {
      errors.amount = '달러 금액은 소수 둘째 자리까지 입력할 수 있습니다.';
    }
    if (!form.startDate) errors.startDate = '첫 결제일을 입력해 주세요.';
    return errors;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length) return;
    setBusy(true);
    setError('');
    try {
      await onSubmit({ ...form, name: form.name.trim(), amount: String(form.amount), memo: form.memo.trim() || null });
    } catch (err) {
      setFieldErrors(err.fieldErrors || {});
      if (!Object.keys(err.fieldErrors || {}).length) setError(err.message);
      setBusy(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="form card" noValidate>
      <Alert>{error}</Alert>
      <Field label="서비스명" error={fieldErrors.name}>
        <input type="text" maxLength={50} placeholder="예: 넷플릭스, ChatGPT Plus" value={form.name} onChange={set('name')} />
      </Field>
      <div className="grid-2">
        <Field label="카테고리" error={fieldErrors.category}>
          <select value={form.category} onChange={set('category')}>
            {CATEGORIES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </Field>
        <Field label="결제 주기" error={fieldErrors.billingCycle}>
          <select value={form.billingCycle} onChange={set('billingCycle')}>
            {CYCLES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </Field>
      </div>
      <div className="grid-2">
        <Field label="통화" error={fieldErrors.currency}>
          <select value={form.currency} onChange={set('currency')}>
            {CURRENCIES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </Field>
        <Field
          label={form.currency === 'USD' ? '결제 금액 ($)' : '결제 금액 (원)'}
          error={fieldErrors.amount}
          hint="가격표가 아니라 실제 청구 금액을 입력하세요. 해외 서비스는 부가세가 붙을 수 있습니다."
        >
          <input
            type="number"
            inputMode="decimal"
            min={form.currency === 'USD' ? '0.01' : '1'}
            step={form.currency === 'USD' ? '0.01' : '1'}
            value={form.amount}
            onChange={set('amount')}
          />
        </Field>
      </div>
      <Field label="첫 결제일" error={fieldErrors.startDate} hint="이 날짜를 기준으로 다음 결제일을 계산합니다.">
        <input type="date" value={form.startDate} onChange={set('startDate')} />
      </Field>
      <Field label="메모 (선택)" error={fieldErrors.memo}>
        <textarea maxLength={200} rows={2} value={form.memo} onChange={set('memo')} />
      </Field>
      <div className="form-actions">
        {onCancel && <button type="button" className="btn btn-ghost" onClick={onCancel} disabled={busy}>취소</button>}
        <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? '저장 중...' : submitText}</button>
      </div>
    </form>
  );
}
