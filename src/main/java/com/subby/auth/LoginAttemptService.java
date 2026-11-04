package com.subby.auth;

import com.subby.global.error.ApiException;
import com.subby.global.error.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public void checkNotLocked(String email) {
        Instant now = clock.instant();
        loginAttemptRepository.findById(email)
                .filter(attempt -> attempt.isLocked(now))
                .ifPresent(attempt -> {
                    throw new ApiException(ErrorCode.LOGIN_LOCKED);
                });
    }

    /** 로그인 API가 예외를 던지기 전에 호출하므로 별도 트랜잭션으로 커밋된다. */
    @Transactional
    public void recordFailure(String email) {
        Instant now = clock.instant();
        LoginAttempt attempt = loginAttemptRepository.findById(email)
                .orElseGet(() -> new LoginAttempt(email, now));
        attempt.recordFailure(now);
        loginAttemptRepository.save(attempt);
    }

    @Transactional
    public void reset(String email) {
        loginAttemptRepository.deleteById(email);
    }
}
