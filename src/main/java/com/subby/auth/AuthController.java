package com.subby.auth;

import com.subby.auth.dto.AuthDtos.LoginRequest;
import com.subby.auth.dto.AuthDtos.PasswordResetConfirm;
import com.subby.auth.dto.AuthDtos.PasswordResetRequest;
import com.subby.auth.dto.AuthDtos.SignupRequest;
import com.subby.auth.password.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그아웃(POST /api/auth/logout)은 Spring Security의 LogoutFilter가 처리한다.
 * 소셜 로그인은 /oauth2/authorization/{google|kakao}로 시작한다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public void signup(@Valid @RequestBody SignupRequest request,
                       HttpServletRequest httpRequest,
                       HttpServletResponse httpResponse) {
        authService.signup(request, httpRequest, httpResponse);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void login(@Valid @RequestBody LoginRequest request,
                      HttpServletRequest httpRequest,
                      HttpServletResponse httpResponse) {
        authService.login(request, httpRequest, httpResponse);
    }

    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.request(request.email());
    }

    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirm request) {
        passwordResetService.confirm(request.token(), request.newPassword());
    }
}
