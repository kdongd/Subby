package com.subby.push;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PushEndpointPolicyTest {

    @ParameterizedTest
    @DisplayName("주요 브라우저의 푸시 서버 주소는 허용한다")
    @ValueSource(strings = {
            "https://fcm.googleapis.com/fcm/send/abc:APA91b",
            "https://updates.push.services.mozilla.com/wpush/v2/gAAAAA",
            "https://web.push.apple.com/QGuQyavXutnMH",
            "https://wns2-sg2p.notify.windows.com/w/?token=abc"
    })
    void allowed(String endpoint) {
        assertThat(PushEndpointPolicy.isAllowed(endpoint)).isTrue();
    }

    @ParameterizedTest
    @DisplayName("그 밖의 주소는 거부한다 (서버를 통한 내부망 접근 방지)")
    @ValueSource(strings = {
            "http://fcm.googleapis.com/fcm/send/abc",
            "https://localhost/push",
            "https://169.254.169.254/latest/meta-data",
            "https://fcm.googleapis.com.evil.com/x",
            "https://evilfcm.googleapis.com.attacker.io/x",
            "https://fakepush.apple.com.example/x",
            "https://user@fcm.googleapis.com/x",
            "https://fcm.googleapis.com:8443/x",
            "ftp://fcm.googleapis.com/x",
            "not a url"
    })
    void rejected(String endpoint) {
        assertThat(PushEndpointPolicy.isAllowed(endpoint)).isFalse();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("VAPID aud는 푸시 서버의 origin이다")
    void audience() {
        assertThat(PushEndpointPolicy.audience("https://Web.Push.Apple.com/abc?x=1")).isEqualTo("https://web.push.apple.com");
    }
}
