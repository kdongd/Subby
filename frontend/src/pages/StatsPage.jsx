import { useEffect, useState } from 'react';
import { Bar, BarChart, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis } from 'recharts';
import { api } from '../api.js';
import { Alert, Loading, PageHeader } from '../components/Common.jsx';
import CategoryBars from '../components/CategoryBars.jsx';
import { ChevronIcon, ChevronLeftIcon } from '../components/Icons.jsx';
import { CATEGORY_COLORS, CATEGORY_LABEL, krw, monthLabel, shiftMonth, thisMonthIso } from '../format.js';

export default function StatsPage() {
  const [yearMonth, setYearMonth] = useState(thisMonthIso());
  const [categories, setCategories] = useState(null);
  const [months, setMonths] = useState(6);
  const [monthly, setMonthly] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    setCategories(null);
    api('/api/stats/categories', { params: { yearMonth } }).then(setCategories).catch((err) => setError(err.message));
  }, [yearMonth]);

  useEffect(() => {
    setMonthly(null);
    api('/api/stats/monthly', { params: { months } }).then(setMonthly).catch((err) => setError(err.message));
  }, [months]);

  const isThisMonth = yearMonth === thisMonthIso();
  const count = categories?.items.reduce((sum, item) => sum + item.count, 0) ?? 0;
  const chartData = monthly?.map((m) => ({
    label: `${Number(m.yearMonth.slice(5))}월`,
    total: m.totalKrw,
    yearMonth: m.yearMonth,
  }));

  return (
    <div className="page">
      <PageHeader title="지출 통계" />
      <Alert>{error}</Alert>

      <section className="card section">
        <div className="month-nav">
          <button type="button" className="icon-btn plain" aria-label="이전 달" onClick={() => setYearMonth(shiftMonth(yearMonth, -1))}>
            <ChevronLeftIcon />
          </button>
          <strong>{monthLabel(yearMonth)}</strong>
          <button type="button" className="icon-btn plain" aria-label="다음 달" disabled={isThisMonth}
                  style={isThisMonth ? { opacity: 0.3 } : undefined}
                  onClick={() => setYearMonth(shiftMonth(yearMonth, 1))}>
            <ChevronIcon size={20} />
          </button>
        </div>

        {!categories ? <Loading /> : (
          <>
            <div className="stats-total">
              <span className="muted">{isThisMonth ? '이번 달' : monthLabel(yearMonth)} 구독 지출</span>
              <strong className="num">{krw(categories.total)}</strong>
            </div>
            {categories.items.length > 0 && (
              <div className="donut-wrap">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie data={categories.items} dataKey="amountKrw" nameKey="category" innerRadius="62%" outerRadius="100%"
                         paddingAngle={2} stroke="none" startAngle={90} endAngle={-270} isAnimationActive={false}>
                      {categories.items.map((item) => <Cell key={item.category} fill={CATEGORY_COLORS[item.category]} />)}
                    </Pie>
                    <Tooltip formatter={(value, name) => [krw(value), CATEGORY_LABEL[name]]} />
                  </PieChart>
                </ResponsiveContainer>
                <div className="donut-center">
                  <span className="muted small">결제</span>
                  <strong>{count}건</strong>
                </div>
              </div>
            )}
            <CategoryBars items={categories.items} />
          </>
        )}
      </section>

      <section className="card section">
        <div className="section-head">
          <h2>월별 추이</h2>
          <div className="segmented light" role="group" aria-label="기간" style={{ width: 150 }}>
            {[6, 12].map((m) => (
              <button key={m} type="button" className={months === m ? 'active' : ''} onClick={() => setMonths(m)}>
                {m}개월
              </button>
            ))}
          </div>
        </div>
        {!monthly ? <Loading /> : (
          <>
            <div style={{ width: '100%', height: 200 }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} margin={{ top: 8, right: 0, left: 0, bottom: 0 }}>
                  <XAxis dataKey="label" tickLine={false} axisLine={false} interval={0}
                         tick={{ fill: '#5b6b86', fontSize: months === 12 ? 10 : 12 }} />
                  <Tooltip
                    formatter={(value) => [krw(value), '결제액']}
                    labelFormatter={(_, payload) => (payload?.[0] ? monthLabel(payload[0].payload.yearMonth) : '')}
                    cursor={{ fill: '#e3edfd' }}
                  />
                  <Bar dataKey="total" radius={[8, 8, 8, 8]} cursor="pointer"
                       onClick={(entry) => {
                         const ym = entry?.yearMonth || entry?.payload?.yearMonth;
                         if (ym) setYearMonth(ym);
                       }}>
                    {chartData.map((d) => <Cell key={d.yearMonth} fill={d.yearMonth === yearMonth ? '#3d74e0' : '#a9c6f2'} />)}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
            <p className="note">막대를 누르면 위에서 그 달의 카테고리별 지출을 볼 수 있어요.</p>
          </>
        )}
      </section>
    </div>
  );
}
