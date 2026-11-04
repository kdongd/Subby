package com.subby.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

    public static final Set<Integer> NOTIFY_DAY_OPTIONS = Set.of(1, 3, 7);
    public static final int NICKNAME_MAX_LENGTH = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이메일 동의를 거부한 카카오 계정은 null이다. */
    private String email;

    /** 소셜 로그인으로만 가입한 계정은 null이다. */
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "notify_enabled", nullable = false)
    private boolean notifyEnabled;

    @Column(name = "notify_days_before", nullable = false)
    private int notifyDaysBefore;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private User(String email, String passwordHash, String nickname) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.notifyEnabled = true;
        this.notifyDaysBefore = 1;
    }

    public static User createLocal(String email, String passwordHash, String nickname) {
        return new User(email, passwordHash, nickname);
    }

    public static User createSocial(String email, String nickname) {
        return new User(email, null, nickname);
    }

    public boolean hasPassword() {
        return passwordHash != null;
    }

    public boolean hasEmail() {
        return email != null;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void updateNotification(boolean enabled, int daysBefore) {
        if (!NOTIFY_DAY_OPTIONS.contains(daysBefore)) {
            throw new IllegalArgumentException("알림 시점은 1, 3, 7일 전 중 하나여야 합니다.");
        }
        this.notifyEnabled = enabled;
        this.notifyDaysBefore = daysBefore;
    }

    public void disableNotification() {
        this.notifyEnabled = false;
    }
}
