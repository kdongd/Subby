package com.subby.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * 실패 사유는 코드로만 화면에 넘긴다(예: EMAIL_ALREADY_REGISTERED, access_denied).
 * 화면이 코드에 맞는 안내 문구를 보여준다.
 */
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String code = exception instanceof OAuth2AuthenticationException oauth2Exception
                ? oauth2Exception.getError().getErrorCode()
                : "SOCIAL_LOGIN_FAILED";
        boolean linkMode = request.getAttribute(SocialOAuth2UserService.LINK_MODE_ATTRIBUTE) != null;
        String page = linkMode ? "/settings" : "/login";
        response.sendRedirect(request.getContextPath() + page + "?error="
                + URLEncoder.encode(code, StandardCharsets.UTF_8));
    }
}
