// 자주 쓰는 구독 서비스 목록. 등록 화면에서 고르면 이름과 카테고리가 채워지고, 목록·상세에서 회사 로고를 보여준다.
// 로고는 이미지 파일을 저장소에 넣지 않고, 화면을 볼 때 각 회사 사이트 아이콘을 불러온다(아래 logoUrl).

export const SERVICE_PRESETS = [
  { name: '넷플릭스', domain: 'netflix.com', category: 'OTT', aliases: ['netflix'] },
  { name: '유튜브 프리미엄', domain: 'youtube.com', category: 'OTT', aliases: ['유튜브', 'youtube'] },
  { name: '디즈니+', domain: 'disneyplus.com', category: 'OTT', aliases: ['디즈니', 'disney'] },
  { name: '티빙', domain: 'tving.com', category: 'OTT', aliases: ['tving'] },
  { name: '쿠팡플레이', domain: 'coupangplay.com', category: 'OTT', aliases: ['coupang play'] },
  { name: '웨이브', domain: 'wavve.com', category: 'OTT', aliases: ['wavve'] },
  { name: '왓챠', domain: 'watcha.com', category: 'OTT', aliases: ['watcha'] },
  { name: '스포티파이', domain: 'spotify.com', category: 'MUSIC', aliases: ['spotify'] },
  { name: '멜론', domain: 'melon.com', category: 'MUSIC', aliases: ['melon'] },
  { name: '애플 뮤직', domain: 'music.apple.com', category: 'MUSIC', aliases: ['apple music', '애플뮤직'] },
  { name: 'ChatGPT Plus', domain: 'chatgpt.com', category: 'AI', aliases: ['chatgpt', '챗gpt', '챗지피티', 'openai'] },
  { name: 'Claude Pro', domain: 'claude.ai', category: 'AI', aliases: ['claude', '클로드'] },
  { name: 'Gemini', domain: 'gemini.google.com', category: 'AI', aliases: ['gemini', '제미나이', 'google ai'] },
  { name: 'Perplexity Pro', domain: 'perplexity.ai', category: 'AI', aliases: ['perplexity', '퍼플렉시티'] },
  { name: 'GitHub Copilot', domain: 'github.com', category: 'SOFTWARE', aliases: ['copilot', 'github', '깃허브'] },
  { name: 'Notion', domain: 'notion.so', category: 'SOFTWARE', aliases: ['notion', '노션'] },
  { name: 'Microsoft 365', domain: 'microsoft.com', category: 'SOFTWARE', aliases: ['microsoft', 'office', '오피스'] },
  { name: 'Adobe', domain: 'adobe.com', category: 'SOFTWARE', aliases: ['adobe', '어도비', 'photoshop', '포토샵'] },
  { name: 'iCloud+', domain: 'icloud.com', category: 'CLOUD', aliases: ['icloud', '아이클라우드'] },
  { name: 'Google One', domain: 'one.google.com', category: 'CLOUD', aliases: ['google one', '구글 원', '구글원'] },
  { name: '네이버 플러스 멤버십', domain: 'naver.com', category: 'SHOPPING', aliases: ['네이버', 'naver'] },
  { name: '쿠팡 와우', domain: 'coupang.com', category: 'SHOPPING', aliases: ['쿠팡', 'coupang', '로켓와우'] },
  { name: '배민클럽', domain: 'baemin.com', category: 'SHOPPING', aliases: ['배민', '배달의민족'] },
  { name: '밀리의 서재', domain: 'millie.co.kr', category: 'NEWS_BOOK', aliases: ['밀리', 'millie'] },
  { name: '리디셀렉트', domain: 'ridibooks.com', category: 'NEWS_BOOK', aliases: ['리디', 'ridi'] },
  { name: 'Nintendo Switch Online', domain: 'nintendo.com', category: 'GAME', aliases: ['nintendo', '닌텐도'] },
  { name: 'PlayStation Plus', domain: 'playstation.com', category: 'GAME', aliases: ['playstation', '플스', '플레이스테이션'] },
  { name: 'Xbox Game Pass', domain: 'xbox.com', category: 'GAME', aliases: ['xbox', '엑스박스', 'game pass', '게임패스'] },
];

function normalize(value) {
  return (value || '').toLowerCase().replace(/\s+/g, ' ').trim();
}

/** 구독 이름으로 알려진 서비스를 찾는다. 사용자가 직접 입력한 이름도 별칭으로 맞춘다. */
export function findService(name) {
  const target = normalize(name);
  if (!target) return null;
  return (
    SERVICE_PRESETS.find((s) => normalize(s.name) === target)
    || SERVICE_PRESETS.find((s) => [s.name, ...s.aliases].some((alias) => target.includes(normalize(alias))))
    || null
  );
}

/** 회사 사이트 아이콘 주소. Google 아이콘 서비스를 거쳐 각 사이트의 공식 아이콘을 받아온다. */
export function logoUrl(domain) {
  return `https://www.google.com/s2/favicons?domain=${encodeURIComponent(domain)}&sz=128`;
}

// 로고가 없는 서비스는 이름 첫 글자를 색 원 안에 보여준다. 이름마다 색이 고정되도록 글자로 색을 고른다.
const INITIAL_COLORS = [
  ['#e3edfd', '#2c5dc2'],
  ['#fff0d6', '#8a4a00'],
  ['#e6f6ee', '#1e7a46'],
  ['#fdecee', '#b0303b'],
  ['#efe8fd', '#5b3bb0'],
  ['#e2f4f7', '#136b78'],
];

export function initialStyle(name) {
  const text = name || '?';
  let hash = 0;
  for (const ch of text) hash = (hash * 31 + ch.codePointAt(0)) % 997;
  const [background, color] = INITIAL_COLORS[hash % INITIAL_COLORS.length];
  return { background, color };
}

export function initialOf(name) {
  const text = (name || '?').trim();
  return Array.from(text)[0]?.toUpperCase() || '?';
}
