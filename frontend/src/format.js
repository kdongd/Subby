export const CATEGORIES = [
  ['OTT', 'OTT'],
  ['MUSIC', '음악'],
  ['AI', 'AI'],
  ['CLOUD', '클라우드·저장소'],
  ['SOFTWARE', '소프트웨어'],
  ['GAME', '게임'],
  ['NEWS_BOOK', '뉴스·도서'],
  ['SHOPPING', '쇼핑 멤버십'],
  ['ETC', '기타'],
];
export const CATEGORY_LABEL = Object.fromEntries(CATEGORIES);

export const CYCLES = [
  ['WEEKLY', '매주'],
  ['MONTHLY', '매월'],
  ['YEARLY', '매년'],
];
export const CYCLE_LABEL = Object.fromEntries(CYCLES);

export const CURRENCIES = [
  ['KRW', '원 (KRW)'],
  ['USD', '달러 (USD)'],
];

export function krw(value) {
  return `${Math.round(Number(value) || 0).toLocaleString('ko-KR')}원`;
}

export function usd(value) {
  return `$${Number(value).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

export function amount(currency, value) {
  return currency === 'USD' ? usd(value) : krw(value);
}

export function shortDate(iso) {
  if (!iso) return '-';
  const d = new Date(`${iso}T00:00:00`);
  return d.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric', weekday: 'short' });
}

export function fullDate(iso) {
  if (!iso) return '-';
  const d = new Date(`${iso}T00:00:00`);
  return d.toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' });
}

export function dDay(days) {
  return days === 0 ? '오늘' : `D-${days}`;
}

/** 한국 시간 기준 오늘 날짜 (YYYY-MM-DD) */
export function todayIso() {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul' }).format(new Date());
}

export function thisMonthIso() {
  return todayIso().slice(0, 7);
}

export function monthLabel(yearMonth) {
  const [y, m] = yearMonth.split('-');
  return `${y}년 ${Number(m)}월`;
}

// 소셜 로그인 실패·연결 실패 코드별 안내 문구
export const SOCIAL_ERROR_MESSAGES = {
  EMAIL_ALREADY_REGISTERED: '이미 가입된 이메일입니다. 기존 방식으로 로그인한 뒤 설정에서 소셜 계정을 연결하세요.',
  IDENTITY_IN_USE: '이미 다른 계정에 연결된 소셜 계정입니다.',
  PROVIDER_ALREADY_LINKED: '이미 같은 종류의 소셜 계정이 연결되어 있습니다.',
  access_denied: '소셜 로그인을 취소했습니다.',
};

export function socialErrorMessage(code) {
  return SOCIAL_ERROR_MESSAGES[code] || '소셜 로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.';
}
