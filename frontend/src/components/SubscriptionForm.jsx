import { useMemo, useState } from 'react';
import { Alert, Field, ServiceLogo } from './Common.jsx';
import { addPeriod, CATEGORIES, CURRENCIES, CYCLES, daysUntil, monthDay, todayIso } from '../format.js';
import { SERVICE_PRESETS } from '../services.js';

const EMPTY = {
  name: '',
  category: 'OTT',
  currency: 'KRW',
  amount: '',
  billingCycle: 'MONTHLY',
  startDate: '',
  memo: '',
};

/**
 * 구독 등록·수정 공용 폼.
 *
 * 날짜는 두 가지 방식으로 받는다. 어느 쪽이든 그 날짜가 결제일 계산의 기준이 된다(서버에는 startDate로 저장).
 * - 이미 구독 중이에요: 구독 앱·앱스토어에 보이는 "다음 결제일(갱신일)"을 넣는다. 이 날짜 전의 결제는 통계에 잡히지 않는다.
 * - 처음 결제해요: 처음 결제한(할) 날짜를 넣는다.
 * 수정할 때는 저장된 기준일을 그대로 보여준다.
 */
export default function SubscriptionForm({ initial, editing = false, submitText, onSubmit }) {
  const [form, setForm] = useState(() => ({ ...EMPTY, ...initial, memo: initial?.memo || '' }));
  const [mode, setMode] = useState('next'); // 'next' | 'first'
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const update = (patch) => setForm((prev) => ({ ...prev, ...patch }));
  const set = (key) => (e) => update({ [key]: e.target.value });

  const pickPreset = (preset) => update({ name: preset.name, category: preset.category });

  const dateLabel = editing ? '결제 기준일' : mode === 'next' ? '다음 결제일 (갱신일)' : '첫 결제일';

  const preview = useMemo(() => {
    if (!form.startDate) return null;
    const dates = [0, 1, 2].map((n) => addPeriod(form.startDate, form.billingCycle, n));
    return dates.map(monthDay).join(' → ');
  }, [form.startDate, form.billingCycle]);

  const validate = () => {
    const errors = {};
    if (!form.name.trim()) errors.name = '서비스 이름을 입력해 주세요.';
    const value = Number(form.amount);
    if (form.amount === '' || Number.isNaN(value) || value <= 0) {
      errors.amount = '결제 금액을 입력해 주세요.';
    } else if (form.currency === 'KRW' && !Number.isInteger(value)) {
      errors.amount = '원화 금액은 정수로 입력해 주세요.';
    } else if (form.currency === 'USD' && !/^\d+(\.\d{1,2})?$/.test(String(form.amount))) {
      errors.amount = '달러 금액은 소수 둘째 자리까지 입력할 수 있어요.';
    }
    if (!form.startDate) errors.startDate = '날짜를 골라 주세요.';
    else if (!editing && mode === 'next' && daysUntil(form.startDate) < 0) {
      errors.startDate = '다음 결제일은 오늘이거나 그 이후여야 해요. 지난 날짜라면 "처음 결제해요"를 골라 주세요.';
    }
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
    <form onSubmit={handleSubmit} className="form" noValidate>
      {!editing && (
        <div className="field">
          <span className="field-label">자주 쓰는 서비스</span>
          <div className="presets">
            {SERVICE_PRESETS.map((preset) => (
              <button key={preset.name} type="button" className={`preset ${form.name === preset.name ? 'active' : ''}`}
                      aria-pressed={form.name === preset.name} onClick={() => pickPreset(preset)}>
                <ServiceLogo name={preset.name} />
                <span>{preset.name}</span>
              </button>
            ))}
          </div>
        </div>
      )}

      <Field label="서비스 이름" error={fieldErrors.name}>
        <input type="text" maxLength={50} placeholder="예: 넷플릭스, ChatGPT Plus" value={form.name} onChange={set('name')} />
      </Field>

      <div className="field">
        <span className="field-label">카테고리</span>
        <div className="chips wrap" role="radiogroup" aria-label="카테고리">
          {CATEGORIES.map(([value, label]) => (
            <button key={value} type="button" role="radio" aria-checked={form.category === value}
                    className={`chip-btn ${form.category === value ? 'active' : ''}`} onClick={() => update({ category: value })}>
              {label}
            </button>
          ))}
        </div>
        {fieldErrors.category && <span className="field-error">{fieldErrors.category}</span>}
      </div>

      <div className="grid-2">
        <Field label="결제 금액" error={fieldErrors.amount}>
          <span className="amount-input">
            <input type="number" inputMode="decimal"
                   min={form.currency === 'USD' ? '0.01' : '1'} step={form.currency === 'USD' ? '0.01' : '1'}
                   placeholder={form.currency === 'USD' ? '20.00' : '17000'}
                   value={form.amount} onChange={set('amount')} />
            <span className="unit">{form.currency === 'USD' ? '$' : '원'}</span>
          </span>
        </Field>
        <div className="field">
          <span className="field-label">통화</span>
          <div className="segmented light" role="radiogroup" aria-label="통화">
            {CURRENCIES.map(([value, label]) => (
              <button key={value} type="button" role="radio" aria-checked={form.currency === value}
                      className={form.currency === value ? 'active' : ''} onClick={() => update({ currency: value })}>
                {label}
              </button>
            ))}
          </div>
        </div>
      </div>
      <p className="field-hint" style={{ marginTop: -10 }}>가격표가 아니라 실제 청구 금액을 넣어 주세요. 해외 서비스는 부가세가 붙을 수 있어요.</p>

      <div className="field">
        <span className="field-label">결제 주기</span>
        <div className="segmented light" role="radiogroup" aria-label="결제 주기">
          {CYCLES.map(([value, label]) => (
            <button key={value} type="button" role="radio" aria-checked={form.billingCycle === value}
                    className={form.billingCycle === value ? 'active' : ''} onClick={() => update({ billingCycle: value })}>
              {label}
            </button>
          ))}
        </div>
      </div>

      {!editing && (
        <div className="field">
          <span className="field-label">결제일 입력 방식</span>
          <div className="segmented" role="radiogroup" aria-label="결제일 입력 방식">
            <button type="button" role="radio" aria-checked={mode === 'next'} className={mode === 'next' ? 'active' : ''}
                    onClick={() => setMode('next')}>
              이미 구독 중이에요
            </button>
            <button type="button" role="radio" aria-checked={mode === 'first'} className={mode === 'first' ? 'active' : ''}
                    onClick={() => setMode('first')}>
              처음 결제해요
            </button>
          </div>
          <div className="bubble">
            <img src="/images/calendar.webp" alt="" width="834" height="847" />
            {mode === 'next' ? (
              <p>구독 앱이나 앱스토어 구독 화면에 보이는 <b>다음 결제일·갱신일</b>을 넣어 주세요. 결제 문자 날짜와 하루 다를 수 있어요.</p>
            ) : (
              <p><b>처음 결제한(할) 날짜</b>를 넣어 주세요. 이 날부터 {form.billingCycle === 'WEEKLY' ? '매주 같은 요일' : form.billingCycle === 'YEARLY' ? '매년 같은 날' : '매달 같은 날'} 결제되는 걸로 계산해요.</p>
            )}
          </div>
        </div>
      )}

      <Field
        label={dateLabel}
        error={fieldErrors.startDate}
        hint={editing ? '이 날짜를 기준으로 결제일을 계산해요. 바꾸면 이전 결제 기록(통계)도 이 날짜부터 다시 계산돼요.' : undefined}
      >
        <input type="date" value={form.startDate} min={!editing && mode === 'next' ? todayIso() : undefined}
               onChange={set('startDate')} />
      </Field>
      {preview && <p className="preview-line">결제일: {preview} …</p>}

      <Field label="메모 (선택)" error={fieldErrors.memo}>
        <textarea maxLength={200} rows={2} placeholder="예: 가족 요금제, 연말에 해지 예정" value={form.memo} onChange={set('memo')} />
      </Field>

      <Alert>{error}</Alert>
      <div className="sticky-actions">
        <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={busy}>
          {busy ? '저장 중...' : submitText}
        </button>
      </div>
    </form>
  );
}
