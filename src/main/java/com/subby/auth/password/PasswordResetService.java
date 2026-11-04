package com.subby.auth.password;

import com.subby.auth.EmailNormalizer;
import com.subby.auth.LoginAttemptService;
import com.subby.global.config.AppProperties;
import com.subby.global.error.ApiException;
import com.subby.global.error.ErrorCode;
import com.subby.global.security.SecureTokens;
import com.subby.mail.MailSender;
import com.subby.mail.MailTemplates;
import com.subby.user.User;
import com.subby.user.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailSender mailSender;
    private final MailTemplates mailTemplates;
    private final LoginAttemptService loginAttemptService;
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;
    private final TransactionTemplate transactionTemplate;
    private final AppProperties properties;
    private final Clock clock;

    /**
     * 가입된 이메일일 때만 메일을 보낸다. 응답은 가입 여부와 상관없이 같아서 가입된 이메일을 알아낼 수 없다.
     */
    public void request(String rawEmail) {
        String email = EmailNormalizer.normalize(rawEmail);
        Optional<IssuedToken> issued = transactionTemplate.execute(status ->
                userRepository.findByEmail(email).map(this::issueToken));
        if (issued == null || issued.isEmpty()) {
            return;
        }
        IssuedToken token = issued.get();
        String link = properties.baseUrl() + "/reset-password?token=" + token.rawToken();
        try {
            mailSender.send(email, mailTemplates.passwordResetSubject(),
                    mailTemplates.passwordReset(token.nickname(), link));
        } catch (RuntimeException e) {
            log.warn("비밀번호 재설정 메일 발송 실패 userId={}", token.userId(), e);
        }
    }

    private IssuedToken issueToken(User user) {
        // 이전에 발급한 링크는 모두 무효화한다.
        tokenRepository.deleteAllByUserId(user.getId());
        String rawToken = SecureTokens.randomUrlToken();
        Instant expiresAt = clock.instant().plus(TOKEN_TTL);
        tokenRepository.save(new PasswordResetToken(user.getId(), SecureTokens.sha256Hex(rawToken), expiresAt));
        return new IssuedToken(user.getId(), user.getNickname(), rawToken);
    }

    public void confirm(String rawToken, String newPassword) {
        Long userId = transactionTemplate.execute(status -> {
            Instant now = clock.instant();
            PasswordResetToken token = tokenRepository.findByTokenHash(SecureTokens.sha256Hex(rawToken))
                    .filter(t -> t.isUsable(now))
                    .orElseThrow(() -> new ApiException(ErrorCode.INVALID_RESET_TOKEN));
            User user = userRepository.findById(token.getUserId())
                    .orElseThrow(() -> new ApiException(ErrorCode.INVALID_RESET_TOKEN));
            user.changePassword(passwordEncoder.encode(newPassword));
            token.markUsed(now);
            if (user.hasEmail()) {
                loginAttemptService.reset(user.getEmail());
            }
            return user.getId();
        });
        // 다른 기기에 남아 있던 로그인도 모두 끊는다.
        sessionRepository.findByPrincipalName(String.valueOf(userId))
                .keySet()
                .forEach(sessionRepository::deleteById);
    }

    private record IssuedToken(Long userId, String nickname, String rawToken) {
    }
}
