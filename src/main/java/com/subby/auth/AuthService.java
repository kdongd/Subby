package com.subby.auth;

import com.subby.auth.dto.AuthDtos.LoginRequest;
import com.subby.auth.dto.AuthDtos.SignupRequest;
import com.subby.global.error.ApiException;
import com.subby.global.error.ErrorCode;
import com.subby.global.security.UserPrincipal;
import com.subby.user.User;
import com.subby.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final LoginAttemptService loginAttemptService;
    private final TransactionTemplate transactionTemplate;

    private final SecurityContextHolderStrategy contextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    /** 가입 후 바로 로그인 상태로 만든다. */
    public void signup(SignupRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String email = EmailNormalizer.normalize(request.email());
        User user = transactionTemplate.execute(status -> {
            if (userRepository.existsByEmail(email)) {
                throw ApiException.invalidField("email", ErrorCode.EMAIL_ALREADY_REGISTERED.message());
            }
            try {
                return userRepository.saveAndFlush(
                        User.createLocal(email, passwordEncoder.encode(request.password()), request.nickname().trim()));
            } catch (DataIntegrityViolationException e) {
                // 같은 이메일로 동시에 가입 요청이 들어온 경우
                throw ApiException.invalidField("email", ErrorCode.EMAIL_ALREADY_REGISTERED.message());
            }
        });
        UserPrincipal principal = UserPrincipal.from(user);
        principal.eraseCredentials();
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        establishSession(authentication, httpRequest, httpResponse);
    }

    public void login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String email = EmailNormalizer.normalize(request.email());
        loginAttemptService.checkNotLocked(email);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        } catch (AuthenticationException e) {
            loginAttemptService.recordFailure(email);
            // 이메일이 없는지 비밀번호가 틀렸는지 구분하지 않는다.
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        loginAttemptService.reset(email);
        establishSession(authentication, httpRequest, httpResponse);
    }

    private void establishSession(Authentication authentication,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        // 로그인 직후 세션 ID를 새로 발급해서 세션 고정 공격을 막는다.
        request.getSession(true);
        request.changeSessionId();

        SecurityContext context = contextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
