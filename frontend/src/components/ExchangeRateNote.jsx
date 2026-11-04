import { fullDate } from '../format.js';

export default function ExchangeRateNote({ info }) {
  if (!info) return null;
  const rate = Number(info.usdKrw).toLocaleString('ko-KR', { maximumFractionDigits: 2 });
  return (
    <p className="note">
      {info.fallback
        ? `환율 정보를 아직 받지 못해 기본 환율(1달러 = ${rate}원)로 계산했어요.`
        : `환율 1달러 = ${rate}원 (${fullDate(info.rateDate)} 유럽중앙은행 기준).`}
      {' '}달러 결제의 원화 금액은 추정치예요.
    </p>
  );
}
