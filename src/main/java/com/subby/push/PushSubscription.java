package com.subby.push;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 브라우저(기기) 1개의 푸시 구독 정보. 한 사용자가 휴대폰, 노트북 등 여러 개를 가질 수 있다. */
@Entity
@Table(name = "push_subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 2048)
    private String endpoint;

    @Column(nullable = false)
    private String p256dh;

    @Column(nullable = false)
    private String auth;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    public PushSubscription(Long userId, String endpoint, String p256dh, String auth, String userAgent, Instant now) {
        this.userId = userId;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.userAgent = userAgent;
        this.createdAt = now;
    }

    /**
     * 같은 브라우저가 다시 구독하면 키가 바뀌었을 수 있으므로 갱신한다.
     * 같은 기기에서 다른 계정으로 로그인해 구독하면 그 기기의 알림은 새 계정으로 옮긴다.
     */
    public void refresh(Long userId, String p256dh, String auth, String userAgent) {
        this.userId = userId;
        this.p256dh = p256dh;
        this.auth = auth;
        this.userAgent = userAgent;
    }

    public void markSuccess(Instant now) {
        this.lastSuccessAt = now;
    }
}
