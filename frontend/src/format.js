export const CATEGORIES = [
  ['OTT', 'OTT'],
  ['MUSIC', '음악'],
  ['AI', 'AI'],
  ['CLOUD', '클라우드'],
  ['SOFTWARE', '소프트웨어'],
  ['GAME', '게임'],
  ['NEWS_BOOK', '뉴스·도서'],
  ['SHOPPING', '쇼핑 멤버십'],
  ['ETC', '기타'],
];
export const CATEGORY_LABEL = Object.fromEntries(CATEGORIES);

/** 카테고리 색. 서로 붙어 있어도 구분되도록 밝기도 다르게 골랐다. */
export const CATEGORY_COLORS = {
  OTT: '#3d74e0',
  MUSIC: '#f2a93b',
  AI: '#20a07a',
  CLOUD: '#8fb2ee',
  SOFTWARE: '#7b5cd6',
  GAME: '#e5627a',
  NEWS_BOOK: '#2d9cb3',
  SHOPPING: '#f4c94a',
  ETC: '#9aa7bf',
};

export const CYCLES = [
  ['WEEKLY', '매주'],
  ['MONTHLY', '매월'],
  ['YEARLY', '매년'],
];
export const CYCLE_LABEL = Object.fromEntries(CYCLES);
/** "월 17,000원"처럼 금액 앞에 붙이는 말 */
export const CYCLE_PREFIX = { WEEKLY: '주', MONTHLY: '월', YEARLY: '연' };

export const CURRENCIES = [
  ['KRW', '원'],
  ['USD', '달러'],
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

/** 결제일까지 남은 날 표시: 오늘 / 내일 / D-3 */
export function dDayLabel(days) {
  if (days === 0) return '오늘';
  if (days === 1) return '내일';
  return `D-${days}`;
}

/** 한국 시간 기준 오늘 날짜 (YYYY-MM-DD) */
export function todayIso() {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul' }).format(new Date());
}

export function thisMonthIso() {
  return todayIso().slice(0, 7);
}

/** 오늘부터 iso 날짜까지 남은 일수 */
export function daysUntil(iso) {
  const today = new Date(`${todayIso()}T00:00:00`);
  const date = new Date(`${iso}T00:00:00`);
  return Math.round((date - today) / 86400000);
}

export function monthLabel(yearMonth) {
  const [y, m] = yearMonth.split('-');
  return `${y}년 ${Number(m)}월`;
}

export function shiftMonth(yearMonth, delta) {
  const [y, m] = yearMonth.split('-').map(Number);
  const date = new Date(Date.UTC(y, m - 1 + delta, 1));
  return `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, '0')}`;
}

/**
 * 기준일에서 n주기 뒤 결제일. 서버 계산(BillingSchedule)과 같은 규칙:
 * 그 달에 같은 날이 없으면 말일, 2월 29일 연간 결제는 평년에 2월 28일.
 */
export function addPeriod(iso, cycle, n) {
  const [y, m, d] = iso.split('-').map(Number);
  if (cycle === 'WEEKLY') {
    const date = new Date(Date.UTC(y, m - 1, d + 7 * n));
    return date.toISOString().slice(0, 10);
  }
  const monthIndex = cycle === 'YEARLY' ? (m - 1) : (m - 1 + n);
  const year = cycle === 'YEARLY' ? y + n : y + Math.floor(monthIndex / 12);
  const month = ((monthIndex % 12) + 12) % 12;
  const lastDay = new Date(Date.UTC(year, month + 1, 0)).getUTCDate();
  const date = new Date(Date.UTC(year, month, Math.min(d, lastDay)));
  return date.toISOString().slice(0, 10);
}

/** "11월 20일"처럼 짧게 */
export function monthDay(iso) {
  const [, m, d] = iso.split('-').map(Number);
  return `${m}월 ${d}일`;
}

// 소셜 로그인 실패·연결 실패 코드별 안내 문구
export const SOCIAL_ERROR_MESSAGES = {
  EMAIL_ALREADY_REGISTERED: '같은 이메일로 가입된 계정이 있어요. 원래 쓰던 방법으로 로그인한 뒤 내 정보에서 계정을 연결해 주세요.',
  IDENTITY_IN_USE: '이미 다른 서비 계정에 연결된 소셜 계정이에요.',
  PROVIDER_ALREADY_LINKED: '이미 같은 종류의 소셜 계정이 연결되어 있어요.',
  access_denied: '로그인을 취소했어요.',
};

export function socialErrorMessage(code) {
  return SOCIAL_ERROR_MESSAGES[code] || '로그인에 실패했어요. 잠시 후 다시 시도해 주세요.';
}

/** 결제일까지 남은 날 표현: 오늘 / 내일 / N일 후 / 지난 결제 */
export function relativeDay(iso) {
  const days = daysUntil(iso);
  if (days < 0) return '지난 결제';
  if (days === 0) return '오늘';
  if (days === 1) return '내일';
  return `${days}일 후`;
}

/** 알림 받은 시각: 방금 / N분 전 / N시간 전 / M월 D일 */
export function timeAgo(isoInstant) {
  const diff = (Date.now() - new Date(isoInstant).getTime()) / 1000;
  if (diff < 60) return '방금';
  if (diff < 3600) return `${Math.floor(diff / 60)}분 전`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`;
  return new Date(isoInstant).toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' });
}
