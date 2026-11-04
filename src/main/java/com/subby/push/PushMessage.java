package com.subby.push;

/**
 * 기기에 표시할 알림 1개. 서비스 워커(sw.js)가 이 값으로 알림을 띄운다.
 *
 * @param tag 같은 tag의 알림은 기기에서 하나로 합쳐진다(새 것이 이전 것을 덮는다).
 * @param url 알림을 누르면 열 화면 경로 (/로 시작)
 */
public record PushMessage(String title, String body, String url, String tag) {
}
