// 서비 서비스 워커: 푸시 알림을 받아 화면에 띄우고, 누르면 해당 화면을 연다.
// 화면 파일을 캐시하지 않는다(오프라인 기능 없음). 배포하면 항상 최신 화면이 뜨게 하기 위해서다.

self.addEventListener('install', () => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener('push', (event) => {
  let data = {};
  try {
    data = event.data ? event.data.json() : {};
  } catch {
    data = { body: event.data ? event.data.text() : '' };
  }
  const title = data.title || '서비';
  const options = {
    body: data.body || '',
    icon: '/icon-192.png',
    badge: '/badge-96.png',
    tag: data.tag || undefined,
    renotify: Boolean(data.tag),
    lang: 'ko',
    data: { url: safePath(data.url) },
  };
  event.waitUntil(
    Promise.all([
      self.registration.showNotification(title, options),
      // 열려 있는 화면에 알려서 🔔 숫자를 바로 갱신하게 한다.
      self.clients.matchAll({ type: 'window', includeUncontrolled: true })
        .then((clients) => clients.forEach((client) => client.postMessage({ type: 'subby:push' }))),
    ]),
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const target = new URL(event.notification.data?.url || '/', self.location.origin).href;
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(async (clients) => {
      // 이미 열린 서비 창이 있으면 그 창에서 이동하고, 없으면 새로 연다.
      for (const client of clients) {
        if (new URL(client.url).origin === self.location.origin && 'focus' in client) {
          await client.focus();
          if ('navigate' in client) return client.navigate(target);
          return undefined;
        }
      }
      return self.clients.openWindow(target);
    }),
  );
});

// 알림 데이터의 주소는 우리 사이트 안의 경로만 허용한다.
function safePath(url) {
  return typeof url === 'string' && url.startsWith('/') && !url.startsWith('//') ? url : '/';
}
