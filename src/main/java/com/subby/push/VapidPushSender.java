package com.subby.push;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

/**
 * 실제 푸시 서버로 보내는 구현. 요청 형식은 RFC 8030(웹 푸시), 8291(암호화), 8292(VAPID)를 따른다.
 * 리다이렉트는 따라가지 않는다. 허용한 푸시 서버 밖으로 요청이 나가지 않게 하기 위해서다.
 */
@Slf4j
public class VapidPushSender implements PushSender {

    /** 기기가 꺼져 있을 때 푸시 서버가 메시지를 보관하는 시간. 결제 알림은 하루 지나면 의미가 줄어든다 */
    private static final Duration TTL = Duration.ofHours(24);
    /** VAPID 서명 유효 시간. 표준상 최대 24시간 */
    private static final Duration JWT_LIFETIME = Duration.ofHours(12);

    private final String publicKey;
    private final String privateKey;
    private final String subject;
    private final Clock clock;
    private final HttpClient httpClient;

    public VapidPushSender(String publicKey, String privateKey, String subject, Clock clock) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
        this.clock = clock;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String publicKey() {
        return publicKey;
    }

    @Override
    public SendResult send(PushSubscription subscription, byte[] payload) {
        String endpoint = subscription.getEndpoint();
        if (!PushEndpointPolicy.isAllowed(endpoint)) {
            return SendResult.GONE;
        }
        byte[] body;
        try {
            body = WebPushCrypto.encrypt(payload,
                    WebPushCrypto.b64Decode(subscription.getP256dh()),
                    WebPushCrypto.b64Decode(subscription.getAuth()));
        } catch (IllegalArgumentException e) {
            // 저장된 키가 깨져 있으면 이 구독으로는 영영 보낼 수 없다.
            return SendResult.GONE;
        }
        long expiresAt = clock.instant().plus(JWT_LIFETIME).getEpochSecond();
        String authorization = WebPushCrypto.vapidAuthorization(
                PushEndpointPolicy.audience(endpoint), subject, expiresAt, publicKey, privateKey);

        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", authorization)
                .header("TTL", String.valueOf(TTL.toSeconds()))
                .header("Urgency", "normal")
                .header("Content-Encoding", "aes128gcm")
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return SendResult.SENT;
            }
            if (status == 404 || status == 410) {
                return SendResult.GONE;
            }
            log.warn("푸시 발송 실패 status={} host={} body={}", status, URI.create(endpoint).getHost(),
                    abbreviate(response.body()));
            return SendResult.FAILED;
        } catch (IOException e) {
            log.warn("푸시 서버 연결 실패 host={}: {}", URI.create(endpoint).getHost(), e.toString());
            return SendResult.FAILED;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SendResult.FAILED;
        }
    }

    private static String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 200 ? value.substring(0, 200) + "..." : value;
    }
}
