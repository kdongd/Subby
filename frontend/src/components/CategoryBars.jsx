import { CATEGORY_COLORS, CATEGORY_LABEL, krw } from '../format.js';

/** 카테고리별 비율을 막대 하나에 이어 붙여 보여주고, 아래에 항목별 금액을 적는다. */
export default function CategoryBars({ items }) {
  if (!items || items.length === 0) {
    return <p className="muted">이 기간에 결제된 구독이 없어요.</p>;
  }
  return (
    <>
      <div className="stackbar" aria-hidden="true">
        {items.map((item) => (
          <span key={item.category} style={{ flex: Math.max(item.ratio, 0.02), background: CATEGORY_COLORS[item.category] }} />
        ))}
      </div>
      <ul className="legend">
        {items.map((item) => (
          <li key={item.category}>
            <span className="swatch" style={{ background: CATEGORY_COLORS[item.category] }} />
            <span className="name">{CATEGORY_LABEL[item.category]} <span className="muted small">{item.count}건</span></span>
            <strong className="num">{krw(item.amountKrw)}</strong>
            <span className="pct num">{Math.round(item.ratio * 100)}%</span>
          </li>
        ))}
      </ul>
    </>
  );
}
