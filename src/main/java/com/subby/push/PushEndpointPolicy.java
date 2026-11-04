package com.subby.push;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

/**
 * 푸시 구독 주소(endpoint) 검사. 서버가 이 주소로 직접 요청을 보내기 때문에,
 * 아무 주소나 받으면 서버를 이용해 내부망이나 다른 사이트를 공격하는 통로(SSRF)가 된다.
 * 그래서 주요 브라우저 회사의 푸시 서버 주소만 허용한다.
 */
public final class PushEndpointPolicy {

    public static final int MAX_LENGTH = 2048;

    /** 정확히 일치하거나, 앞에 점을 붙인 형태(.mozilla.com)로 끝나는 호스트를 허용한다. */
    private static final List<String> ALLOWED_HOSTS = List.of(
            "fcm.googleapis.com",          // Chrome, Edge(일부), 삼성 인터넷 등 Chromium 계열
            "android.googleapis.com",      // 예전 Chrome
            "push.services.mozilla.com",   // Firefox
            "push.apple.com",              // Safari (macOS, iOS 홈 화면 앱)
            "notify.windows.com"           // Edge (Windows)
    );

    private PushEndpointPolicy() {
    }

    public static boolean isAllowed(String endpoint) {
        if (endpoint == null || endpoint.length() > MAX_LENGTH) {
            return false;
        }
        URI uri;
        try {
            uri = new URI(endpoint);
        } catch (URISyntaxException e) {
            return false;
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
            return false;
        }
        if (uri.getPort() != -1 && uri.getPort() != 443) {
            return false;
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        return ALLOWED_HOSTS.stream().anyMatch(allowed -> host.equals(allowed) || host.endsWith("." + allowed));
    }

    /** VAPID 서명의 aud 값. 푸시 서버의 origin(https://호스트) */
    public static String audience(String endpoint) {
        URI uri = URI.create(endpoint);
        return uri.getScheme().toLowerCase(Locale.ROOT) + "://" + uri.getHost().toLowerCase(Locale.ROOT);
    }
}
