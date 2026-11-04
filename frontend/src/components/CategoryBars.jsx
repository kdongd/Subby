import { CATEGORY_LABEL, krw } from '../format.js';

export default function CategoryBars({ items }) {
  if (!items || items.length === 0) {
    return <p className="muted">이 기간에 결제된 구독이 없습니다.</p>;
  }
  return (
    <ul className="bars">
      {items.map((item) => (
        <li key={item.category}>
          <div className="bar-head">
            <span>{CATEGORY_LABEL[item.category]} <span className="muted small">{item.count}건</span></span>
            <span>{krw(item.amountKrw)} <span className="muted small">{Math.round(item.ratio * 100)}%</span></span>
          </div>
          <div className="bar-track">
            <div className="bar-fill" style={{ width: `${Math.max(item.ratio * 100, 2)}%` }} />
          </div>
        </li>
      ))}
    </ul>
  );
}
