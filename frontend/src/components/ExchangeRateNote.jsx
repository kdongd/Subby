import { fullDate } from '../format.js';

export default function ExchangeRateNote({ info }) {
  if (!info) return null;
  const rate = Number(info.usdKrw).toLocaleString('ko-KR', { maximumFractionDigits: 2 });
  return (
    <p className="muted small note">
      {info.fallback
        ? `환율 정보를 아직 받지 못해 기본 환율(1달러 = ${rate}원)로 계산했습니다.`
        : `환율 1달러 = ${rate}원 (${fullDate(info.rateDate)} 유럽중앙은행 기준).`}
      {' '}달러 결제의 원화 금액은 추정치이며 실제 카드 청구액과 다를 수 있습니다.
    </p>
  );
}
