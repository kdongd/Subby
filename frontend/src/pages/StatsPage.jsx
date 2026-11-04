import { useEffect, useState } from 'react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api } from '../api.js';
import { Alert, Loading } from '../components/Common.jsx';
import CategoryBars from '../components/CategoryBars.jsx';
import { krw, monthLabel, thisMonthIso } from '../format.js';

function compactKrw(value) {
  if (value >= 10000) return `${Math.round(value / 1000) / 10}만`;
  return value.toLocaleString('ko-KR');
}

export default function StatsPage() {
  const [months, setMonths] = useState(6);
  const [monthly, setMonthly] = useState(null);
  const [yearMonth, setYearMonth] = useState(thisMonthIso());
  const [categories, setCategories] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    setMonthly(null);
    api('/api/stats/monthly', { params: { months } }).then(setMonthly).catch((err) => setError(err.message));
  }, [months]);

  useEffect(() => {
    setCategories(null);
    api('/api/stats/categories', { params: { yearMonth } }).then(setCategories).catch((err) => setError(err.message));
  }, [yearMonth]);

  const chartData = monthly?.map((m) => ({
    label: `${Number(m.yearMonth.slice(5))}월`,
    total: m.totalKrw,
    yearMonth: m.yearMonth,
  }));

  return (
    <div className="stack">
      <h1>지출 통계</h1>
      <Alert>{error}</Alert>

      <section className="card">
        <div className="section-head">
          <h2>월별 구독 지출</h2>
          <div className="segmented" role="group" aria-label="기간">
            {[6, 12].map((m) => (
              <button key={m} type="button" className={months === m ? 'active' : ''} onClick={() => setMonths(m)}>
                {m}개월
              </button>
            ))}
          </div>
        </div>
        {!monthly ? <Loading /> : (
          <div className="chart">
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={chartData} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--line)" />
                <XAxis dataKey="label" tickLine={false} axisLine={false} tick={{ fill: 'var(--muted)', fontSize: 12 }} />
                <YAxis tickFormatter={compactKrw} tickLine={false} axisLine={false} width={48}
                       tick={{ fill: 'var(--muted)', fontSize: 12 }} />
                <Tooltip
                  formatter={(value) => [krw(value), '결제액']}
                  labelFormatter={(_, payload) => (payload?.[0] ? monthLabel(payload[0].payload.yearMonth) : '')}
                  cursor={{ fill: 'var(--hover)' }}
                />
                <Bar dataKey="total" fill="var(--primary)" radius={[4, 4, 0, 0]}
                     cursor="pointer"
                     onClick={(entry) => {
                       const ym = entry?.yearMonth || entry?.payload?.yearMonth;
                       if (ym) setYearMonth(ym);
                     }} />
              </BarChart>
            </ResponsiveContainer>
            <p className="muted small">막대를 누르면 그 달의 카테고리별 지출을 아래에서 볼 수 있습니다.</p>
          </div>
        )}
      </section>

      <section className="card">
        <div className="section-head">
          <h2>{monthLabel(yearMonth)} 카테고리별 지출</h2>
          <input type="month" value={yearMonth} max={thisMonthIso()} onChange={(e) => e.target.value && setYearMonth(e.target.value)} />
        </div>
        {!categories ? <Loading /> : (
          <>
            <p className="total-line">합계 <strong>{krw(categories.total)}</strong></p>
            <CategoryBars items={categories.items} />
          </>
        )}
      </section>
    </div>
  );
}
