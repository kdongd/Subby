package com.subby.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이메일별 연속 로그인 실패 기록. 가입되지 않은 이메일도 똑같이 기록해서 잠금 여부로 가입 여부가 드러나지 않게 한다.
 */
@Entity
@Table(name = "login_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginAttempt {

    public static final int MAX_FAILURES = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    @Id
    private String email;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public LoginAttempt(String email, Instant now) {
        this.email = email;
        this.failedCount = 0;
        this.updatedAt = now;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    /** 실패를 한 번 기록한다. 5회째 실패하면 15분 동안 잠그고, 잠금이 풀린 뒤에는 처음부터 다시 센다. */
    public void recordFailure(Instant now) {
        if (lockedUntil != null && !lockedUntil.isAfter(now)) {
            lockedUntil = null;
            failedCount = 0;
        }
        failedCount++;
        if (failedCount >= MAX_FAILURES) {
            lockedUntil = now.plus(LOCK_DURATION);
            failedCount = 0;
        }
        updatedAt = now;
    }
}
