package com.subby.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CurrencyCode currency;

    /** 금액은 double을 쓰지 않는다. 0.1 같은 값을 정확히 담지 못해 합계가 틀어진다. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    private BillingCycle billingCycle;

    /** 첫 결제일. 모든 결제일 계산의 기준 */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(name = "canceled_at")
    private LocalDate canceledAt;

    private String memo;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private Subscription(Long userId, SubscriptionDetails details) {
        this.userId = userId;
        this.status = SubscriptionStatus.ACTIVE;
        apply(details);
    }

    public static Subscription create(Long userId, SubscriptionDetails details) {
        return new Subscription(userId, details);
    }

    public void update(SubscriptionDetails details) {
        if (canceledAt != null && canceledAt.isBefore(details.startDate())) {
            throw new IllegalArgumentException("첫 결제일은 해지일보다 늦을 수 없습니다.");
        }
        apply(details);
    }

    public void cancel(LocalDate canceledAt) {
        if (status == SubscriptionStatus.CANCELED) {
            throw new IllegalStateException("이미 해지된 구독입니다.");
        }
        if (canceledAt.isBefore(startDate)) {
            throw new IllegalArgumentException("해지일은 첫 결제일 이후여야 합니다.");
        }
        this.status = SubscriptionStatus.CANCELED;
        this.canceledAt = canceledAt;
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }

    public List<LocalDate> paymentDatesBetween(LocalDate from, LocalDate to) {
        return BillingSchedule.between(startDate, billingCycle, canceledAt, from, to);
    }

    public Optional<LocalDate> nextPaymentDate(LocalDate today) {
        return BillingSchedule.nextOnOrAfter(startDate, billingCycle, canceledAt, today);
    }

    public List<LocalDate> upcomingPaymentDates(LocalDate today, int count) {
        return BillingSchedule.upcoming(startDate, billingCycle, canceledAt, today, count);
    }

    private void apply(SubscriptionDetails details) {
        this.name = details.name();
        this.category = details.category();
        this.currency = details.currency();
        this.amount = details.amount();
        this.billingCycle = details.billingCycle();
        this.startDate = details.startDate();
        this.memo = details.memo();
    }
}
