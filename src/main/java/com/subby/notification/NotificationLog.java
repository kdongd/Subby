package com.subby.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 결제 1건(구독 + 결제일)에 대해 알림을 보냈다는 기록. DB 유니크 제약으로 중복 발송을 막는다. */
@Entity
@Table(name = "notification_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    public NotificationLog(Long subscriptionId, LocalDate paymentDate, Instant sentAt) {
        this.subscriptionId = subscriptionId;
        this.paymentDate = paymentDate;
        this.sentAt = sentAt;
    }
}
