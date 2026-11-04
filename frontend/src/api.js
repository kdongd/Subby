// 서버 API 호출. 모든 요청은 같은 주소(쿠키 세션)로 보낸다.
// 쓰기 요청에는 서버가 내려준 XSRF-TOKEN 쿠키 값을 X-XSRF-TOKEN 헤더에 담는다 (CSRF 방어).

export class ApiError extends Error {
  constructor(status, body) {
    super(body?.detail || '요청을 처리하지 못했습니다.');
    this.status = status;
    this.code = body?.code;
    this.fieldErrors = Object.fromEntries((body?.errors || []).map((e) => [e.field, e.message]));
  }
}

function readCookie(name) {
  const match = document.cookie.split('; ').find((row) => row.startsWith(`${name}=`));
  return match ? decodeURIComponent(match.split('=')[1]) : null;
}

// 401을 받으면 로그인 화면으로 보내기 위한 콜백 (auth.jsx에서 등록)
let onUnauthorized = null;
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

export async function api(path, { method = 'GET', body, params, skipAuthRedirect = false } = {}) {
  const url = params
    ? `${path}?${new URLSearchParams(Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== ''))}`
    : path;
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (method !== 'GET') {
    const token = readCookie('XSRF-TOKEN');
    if (token) headers['X-XSRF-TOKEN'] = token;
  }

  let response;
  try {
    response = await fetch(url, {
      method,
      headers,
      credentials: 'same-origin',
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, { detail: '서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.' });
  }

  if (response.status === 204 || response.status === 202) return null;
  const isJson = (response.headers.get('content-type') || '').includes('json');
  const data = isJson ? await response.json() : null;

  if (!response.ok) {
    if (response.status === 401 && !skipAuthRedirect && onUnauthorized) onUnauthorized();
    throw new ApiError(response.status, data);
  }
  return data;
}
