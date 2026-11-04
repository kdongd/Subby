// 웹 푸시: 서비스 워커 등록, 이 기기 구독/해제, 지원 여부 판단.
import { api } from './api.js';

export function registerServiceWorker() {
  if (!('serviceWorker' in navigator)) return;
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {
      // 등록 실패해도 사이트 사용에는 문제없다. 푸시만 안 된다.
    });
  });
}

export function isIos() {
  const ua = navigator.userAgent;
  return /iPhone|iPad|iPod/.test(ua) || (ua.includes('Macintosh') && navigator.maxTouchPoints > 1);
}

export function isStandalone() {
  return window.matchMedia?.('(display-mode: standalone)').matches || window.navigator.standalone === true;
}

/**
 * 이 기기의 푸시 지원 상태
 * - supported: 바로 켤 수 있음
 * - ios-install: iPhone/iPad 브라우저. 홈 화면에 추가한 앱에서만 가능
 * - unsupported: 이 브라우저는 웹 푸시를 지원하지 않음
 * - insecure: https가 아님 (localhost 제외)
 */
export function pushSupport() {
  if (!window.isSecureContext) return 'insecure';
  const hasApi = 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window;
  if (isIos() && !isStandalone()) return 'ios-install';
  return hasApi ? 'supported' : 'unsupported';
}

export function permission() {
  return 'Notification' in window ? Notification.permission : 'denied';
}

async function registration() {
  const existing = await navigator.serviceWorker.getRegistration('/');
  if (existing) return existing;
  await navigator.serviceWorker.register('/sw.js');
  return navigator.serviceWorker.ready;
}

/** 이 기기의 현재 구독 (없으면 null) */
export async function currentSubscription() {
  if (pushSupport() !== 'supported') return null;
  const reg = await navigator.serviceWorker.getRegistration('/');
  return reg ? reg.pushManager.getSubscription() : null;
}

function base64UrlToBytes(value) {
  const padded = (value + '='.repeat((4 - (value.length % 4)) % 4)).replace(/-/g, '+').replace(/_/g, '/');
  const raw = atob(padded);
  return Uint8Array.from(raw, (c) => c.charCodeAt(0));
}

function sameKey(subscription, publicKey) {
  const current = subscription.options?.applicationServerKey;
  if (!current) return true;
  const a = new Uint8Array(current);
  const b = base64UrlToBytes(publicKey);
  return a.length === b.length && a.every((v, i) => v === b[i]);
}

/** 이 기기에서 푸시 받기. 권한을 묻고, 브라우저 구독을 만들어 서버에 등록한다. */
export async function enablePush() {
  const { enabled, publicKey } = await api('/api/push/public-key');
  if (!enabled) throw new Error('서버에 푸시 알림이 설정되지 않았습니다.');

  const result = await Notification.requestPermission();
  if (result !== 'granted') {
    throw new Error(result === 'denied'
      ? '알림 권한이 차단돼 있습니다. 브라우저(또는 휴대폰) 설정에서 이 사이트의 알림을 허용해 주세요.'
      : '알림 권한을 허용해야 푸시를 받을 수 있습니다.');
  }

  const reg = await registration();
  let subscription = await reg.pushManager.getSubscription();
  // 서버 키가 바뀌었으면 예전 구독으로는 받을 수 없으므로 새로 만든다.
  if (subscription && !sameKey(subscription, publicKey)) {
    await subscription.unsubscribe();
    subscription = null;
  }
  if (!subscription) {
    subscription = await reg.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: base64UrlToBytes(publicKey),
    });
  }
  const json = subscription.toJSON();
  await api('/api/push/subscriptions', {
    method: 'POST',
    body: { endpoint: json.endpoint, keys: { p256dh: json.keys.p256dh, auth: json.keys.auth } },
  });
}

/** 이 기기에서 푸시 끄기. 서버 등록을 지우고 브라우저 구독도 해제한다. */
export async function disablePush() {
  const subscription = await currentSubscription();
  if (!subscription) return;
  try {
    await api('/api/push/unsubscribe', { method: 'POST', body: { endpoint: subscription.endpoint } });
  } finally {
    await subscription.unsubscribe();
  }
}

/** 이 기기가 현재 계정으로 푸시를 받는지 */
export async function pushStatus() {
  const subscription = await currentSubscription();
  const status = await api('/api/push/status', {
    method: 'POST',
    body: { endpoint: subscription?.endpoint ?? null },
  });
  return { ...status, subscribed: Boolean(subscription) && status.subscribed };
}

/**
 * 로그아웃 직전에 호출. 공용 기기에서 다음 사람이 내 결제 알림을 받지 않게 서버 등록만 지운다.
 * 브라우저 구독은 남겨 두고, 다음에 로그인한 계정이 켜면 그 계정으로 옮겨진다.
 */
export async function detachPushOnLogout() {
  try {
    const subscription = await currentSubscription();
    if (subscription) {
      await api('/api/push/unsubscribe', {
        method: 'POST',
        body: { endpoint: subscription.endpoint },
        skipAuthRedirect: true,
      });
    }
  } catch {
    // 로그아웃은 계속 진행한다.
  }
}
