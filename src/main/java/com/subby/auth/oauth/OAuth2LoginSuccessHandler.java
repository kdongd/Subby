package com.subby.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/** 소셜 로그인 성공 시 대시보드로, 연결 성공 시 설정 화면으로 보낸다. */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        Object linked = request.getAttribute(SocialOAuth2UserService.LINK_MODE_ATTRIBUTE);
        String target = linked != null
                ? "/settings?linked=" + linked.toString().toLowerCase(Locale.ROOT)
                : "/";
        response.sendRedirect(request.getContextPath() + target);
    }
}
